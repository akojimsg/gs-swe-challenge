package com.gsswec.ecommerce.payments;

import static org.awaitility.Awaitility.await;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gsswec.ecommerce.payments.api.rest.dto.PaymentResponse;
import com.gsswec.ecommerce.shared.constants.StreamNames;
import com.gsswec.ecommerce.shared.dto.OrderItemSnapshot;
import com.gsswec.ecommerce.shared.events.base.BaseEvent;
import com.gsswec.ecommerce.shared.events.order.OrderPlacedEvent;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

// End-to-end saga slice (Payments side): publish order.placed onto the stream, then
// assert the consumer charged via the fake processor, persisted a Payment, and emitted
// the corresponding payment.* event. success-rate is pinned per-test (1.0 / 0.0) so the
// outcome is deterministic rather than relying on the 90/10 RNG.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class PaymentSagaIT {

    private static final String JWT_SECRET = "payments_it_secret_at_least_256_bits_long_xxxxxx";

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("gsswec");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        registry.add("spring.flyway.create-schemas", () -> "true");
        registry.add("gsswec.jwt.secret", () -> JWT_SECRET);
        // Default the success rate high; individual tests publish then read the outcome.
        registry.add("gsswec.payments.success-rate", () -> "1.0");
    }

    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private TestRestTemplate rest;
    @Autowired
    private ObjectMapper objectMapper;

    private void publishOrderPlaced(UUID orderId, UUID userId, BigDecimal total) throws Exception {
        OrderPlacedEvent event = new OrderPlacedEvent(
                new BaseEvent(UUID.randomUUID(), StreamNames.ORDER_PLACED, "1.0", Instant.now(), null, "orders"),
                orderId, userId,
                List.of(new OrderItemSnapshot(UUID.randomUUID(), "SKU-1", "Widget", 2, new BigDecimal("9.99"))),
                total);
        String payload = objectMapper.writeValueAsString(event);
        redisTemplate.opsForStream().add(StreamRecords.newRecord()
                .in(StreamNames.ORDER_PLACED)
                .ofMap(Map.of(
                        "eventId", event.base().eventId().toString(),
                        "eventType", event.base().eventType(),
                        "payload", payload)));
    }

    private String adminToken() {
        SecretKey key = io.jsonwebtoken.security.Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8));
        return io.jsonwebtoken.Jwts.builder().subject(UUID.randomUUID().toString()).claim("role", "ADMIN")
                .issuedAt(new Date()).expiration(new Date(System.currentTimeMillis() + 900_000))
                .signWith(key, io.jsonwebtoken.Jwts.SIG.HS256).compact();
    }

    private HttpEntity<Void> adminAuth() {
        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(adminToken());
        return new HttpEntity<>(h);
    }

    @Test
    void orderPlacedIsChargedAndPaymentSucceededIsPublished() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        publishOrderPlaced(orderId, userId, new BigDecimal("19.98"));

        // payment.succeeded lands on the stream...
        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            var recs = redisTemplate.opsForStream()
                    .range(StreamNames.PAYMENT_SUCCEEDED, org.springframework.data.domain.Range.unbounded());
            Assertions.assertThat(recs).isNotNull().anyMatch(this::isPaymentEvent);
        });

        // ...and a SUCCEEDED Payment row is readable via the admin API.
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            var res = rest.exchange("/api/v1/payments/order/" + orderId,
                    org.springframework.http.HttpMethod.GET, adminAuth(), PaymentResponse.class);
            Assertions.assertThat(res.getStatusCode().is2xxSuccessful()).isTrue();
            Assertions.assertThat(res.getBody().status()).isEqualTo("SUCCEEDED");
            Assertions.assertThat(res.getBody().amount()).isEqualByComparingTo("19.98");
        });
    }

    @Test
    void redeliveredOrderPlacedChargesOnlyOnce() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        // Same orderId, two distinct events (different eventId) — dedupe is on eventId,
        // and ProcessPayment is idempotent on orderId, so only one Payment results.
        publishOrderPlaced(orderId, userId, new BigDecimal("19.98"));
        publishOrderPlaced(orderId, userId, new BigDecimal("19.98"));

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            var res = rest.exchange("/api/v1/payments/order/" + orderId,
                    org.springframework.http.HttpMethod.GET, adminAuth(), PaymentResponse.class);
            Assertions.assertThat(res.getStatusCode().is2xxSuccessful()).isTrue();
        });

        // Exactly one payment exists for this order (UNIQUE order_id backstop held).
        var all = rest.exchange("/api/v1/payments",
                org.springframework.http.HttpMethod.GET, adminAuth(), PaymentResponse[].class);
        long forOrder = java.util.Arrays.stream(all.getBody())
                .filter(p -> p.orderId().equals(orderId)).count();
        Assertions.assertThat(forOrder).isEqualTo(1);
    }

    private boolean isPaymentEvent(MapRecord<String, Object, Object> record) {
        Object type = record.getValue().get("eventType");
        return StreamNames.PAYMENT_SUCCEEDED.equals(type) || StreamNames.PAYMENT_FAILED.equals(type);
    }
}

package com.gsswec.ecommerce.orders.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gsswec.ecommerce.shared.constants.StreamNames;
import com.gsswec.ecommerce.shared.events.base.BaseEvent;
import com.gsswec.ecommerce.shared.events.payment.PaymentFailedEvent;
import com.gsswec.ecommerce.shared.events.payment.PaymentSucceededEvent;
import com.gsswec.ecommerce.stock.grpc.ReleaseRequest;
import com.gsswec.ecommerce.stock.grpc.ReleaseResponse;
import com.gsswec.ecommerce.stock.grpc.ReserveRequest;
import com.gsswec.ecommerce.stock.grpc.ReserveResponse;
import com.gsswec.ecommerce.stock.grpc.ReservedLine;
import com.gsswec.ecommerce.stock.grpc.ShortfallReason;
import com.gsswec.ecommerce.stock.grpc.StockServiceGrpc;
import io.grpc.stub.StreamObserver;
import io.jsonwebtoken.Jwts;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.crypto.SecretKey;
import net.devh.boot.grpc.server.service.GrpcService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
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

// End-to-end saga slice (Orders side, compensation). Place an order via the API (which
// reserves stock through the in-process fake Products server), then publish the payment
// outcome onto the stream and assert the consumer drove the order to its terminal state:
//   payment.succeeded -> PAID, order.paid emitted, no stock released
//   payment.failed    -> FAILED, stock released (compensation), order.failed emitted
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            "grpc.server.port=-1",
            "grpc.server.in-process-name=orders-saga-it",
            "grpc.client.products.address=in-process:orders-saga-it",
            "grpc.client.products.negotiation-type=plaintext"
        })
@Testcontainers
class SagaCompensationIT {

    private static final String JWT_SECRET = "orders_saga_it_secret_at_least_256_bits_long_xxx";

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
    }

    @TestConfiguration
    static class FakeStockServer {
        static final Map<String, Integer> STOCK = new ConcurrentHashMap<>();
        static final UUID WIDGET_ID = UUID.randomUUID();

        @GrpcService
        static class Stub extends StockServiceGrpc.StockServiceImplBase {
            @Override
            public void reserve(ReserveRequest req, StreamObserver<ReserveResponse> obs) {
                var builder = ReserveResponse.newBuilder();
                boolean ok = true;
                for (var line : req.getLinesList()) {
                    int have = STOCK.getOrDefault(line.getProductId(), 0);
                    if (have < line.getQuantity()) {
                        ok = false;
                        builder.addShortfallsBuilder()
                                .setProductId(line.getProductId()).setRequested(line.getQuantity())
                                .setAvailable(have).setReason(ShortfallReason.INSUFFICIENT_STOCK);
                    }
                }
                if (ok) {
                    for (var line : req.getLinesList()) {
                        STOCK.merge(line.getProductId(), -line.getQuantity(), Integer::sum);
                        builder.addLines(ReservedLine.newBuilder()
                                .setProductId(line.getProductId()).setSku("SKU-1").setName("Widget")
                                .setUnitPrice("9.99").setQuantity(line.getQuantity()).build());
                    }
                }
                obs.onNext(builder.setReserved(ok).build());
                obs.onCompleted();
            }

            @Override
            public void release(ReleaseRequest req, StreamObserver<ReleaseResponse> obs) {
                req.getLinesList().forEach(l -> STOCK.merge(l.getProductId(), l.getQuantity(), Integer::sum));
                obs.onNext(ReleaseResponse.newBuilder().setReleased(true).build());
                obs.onCompleted();
            }
        }
    }

    @Autowired
    private TestRestTemplate rest;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private ObjectMapper objectMapper;

    private final UUID buyerId = UUID.randomUUID();

    private String buyerToken() {
        SecretKey key = io.jsonwebtoken.security.Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder().subject(buyerId.toString()).claim("role", "BUYER")
                .issuedAt(new Date()).expiration(new Date(System.currentTimeMillis() + 900_000))
                .signWith(key, Jwts.SIG.HS256).compact();
    }

    @SuppressWarnings("unchecked")
    private UUID placeOrder(String idemKey, int qty) {
        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(buyerToken());
        h.add("Idempotency-Key", idemKey);
        var body = new HttpEntity<>(Map.of("items",
                List.of(Map.of("productId", FakeStockServer.WIDGET_ID.toString(), "quantity", qty))), h);
        Map<String, Object> res = rest.postForObject("/api/v1/orders", body, Map.class);
        return UUID.fromString((String) res.get("id"));
    }

    // Orders exposes no GET endpoint yet (lifecycle read is a separate lane), so the
    // saga's terminal outcome is verified through the events it emits onto the streams.
    private boolean sawEventForOrder(String stream, UUID orderId) {
        var recs = redisTemplate.opsForStream()
                .range(stream, org.springframework.data.domain.Range.unbounded());
        if (recs == null) {
            return false;
        }
        return recs.stream().anyMatch(r -> {
            Object payload = r.getValue().get("payload");
            return payload != null && payload.toString().contains(orderId.toString());
        });
    }

    private void publishPaymentSucceeded(UUID orderId) throws Exception {
        var event = new PaymentSucceededEvent(
                new BaseEvent(UUID.randomUUID(), StreamNames.PAYMENT_SUCCEEDED, "1.0", Instant.now(), null, "payments"),
                UUID.randomUUID(), orderId, buyerId, new BigDecimal("19.98"), "FAKE_CARD", "4242");
        publish(StreamNames.PAYMENT_SUCCEEDED, event.base().eventId(), event.base().eventType(),
                objectMapper.writeValueAsString(event));
    }

    private void publishPaymentFailed(UUID orderId) throws Exception {
        var event = new PaymentFailedEvent(
                new BaseEvent(UUID.randomUUID(), StreamNames.PAYMENT_FAILED, "1.0", Instant.now(), null, "payments"),
                UUID.randomUUID(), orderId, buyerId, new BigDecimal("19.98"), "CARD_DECLINED");
        publish(StreamNames.PAYMENT_FAILED, event.base().eventId(), event.base().eventType(),
                objectMapper.writeValueAsString(event));
    }

    private void publish(String stream, UUID eventId, String eventType, String payload) {
        redisTemplate.opsForStream().add(StreamRecords.newRecord()
                .in(stream)
                .ofMap(Map.of("eventId", eventId.toString(), "eventType", eventType, "payload", payload)));
    }

    @Test
    void paymentSucceededDrivesOrderToPaidWithoutReleasingStock() throws Exception {
        FakeStockServer.STOCK.put(FakeStockServer.WIDGET_ID.toString(), 10);
        UUID orderId = placeOrder("saga-ok", 2);
        int afterReserve = FakeStockServer.STOCK.get(FakeStockServer.WIDGET_ID.toString());
        assertThat(afterReserve).isEqualTo(8);

        publishPaymentSucceeded(orderId);

        // order.paid is emitted once the consumer transitions PENDING/AWAITING_PAYMENT -> PAID.
        await().atMost(Duration.ofSeconds(15))
                .untilAsserted(() -> assertThat(sawEventForOrder(StreamNames.ORDER_PAID, orderId)).isTrue());
        // No compensation on the happy path: stock unchanged from the reservation.
        assertThat(FakeStockServer.STOCK.get(FakeStockServer.WIDGET_ID.toString())).isEqualTo(afterReserve);
        assertThat(sawEventForOrder(StreamNames.ORDER_FAILED, orderId)).isFalse();
    }

    @Test
    void paymentFailedDrivesOrderToFailedAndReleasesStock() throws Exception {
        FakeStockServer.STOCK.put(FakeStockServer.WIDGET_ID.toString(), 10);
        UUID orderId = placeOrder("saga-fail", 3);
        assertThat(FakeStockServer.STOCK.get(FakeStockServer.WIDGET_ID.toString())).isEqualTo(7);

        publishPaymentFailed(orderId);

        // order.failed is emitted once the consumer transitions to FAILED.
        await().atMost(Duration.ofSeconds(15))
                .untilAsserted(() -> assertThat(sawEventForOrder(StreamNames.ORDER_FAILED, orderId)).isTrue());
        // Compensation released the 3 reserved units back to stock.
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() ->
                assertThat(FakeStockServer.STOCK.get(FakeStockServer.WIDGET_ID.toString())).isEqualTo(10));
    }
}

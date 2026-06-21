package com.gsswec.ecommerce.orders.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.gsswec.ecommerce.stock.grpc.ReleaseRequest;
import com.gsswec.ecommerce.stock.grpc.ReleaseResponse;
import com.gsswec.ecommerce.stock.grpc.ReserveRequest;
import com.gsswec.ecommerce.stock.grpc.ReserveResponse;
import com.gsswec.ecommerce.stock.grpc.ReservedLine;
import com.gsswec.ecommerce.stock.grpc.ShortfallReason;
import com.gsswec.ecommerce.stock.grpc.StockServiceGrpc;
import io.grpc.stub.StreamObserver;
import io.jsonwebtoken.Jwts;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.crypto.SecretKey;
import net.devh.boot.grpc.server.service.GrpcService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            "grpc.server.in-process-name=orders-it",
            "grpc.client.products.address=in-process:orders-it",
            "grpc.client.products.negotiation-type=plaintext"
        })
@Testcontainers
class PlaceOrderIT {

    private static final String JWT_SECRET = "orders_it_secret_at_least_256_bits_long_xxxxxxxx";

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

    // In-process fake Products stock server: real decrement semantics over a map,
    // returning reserved-line details exactly like the real service.
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

    private final UUID buyerId = UUID.randomUUID();

    private String buyerToken() {
        SecretKey key = io.jsonwebtoken.security.Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder().subject(buyerId.toString()).claim("role", "BUYER")
                .issuedAt(new Date()).expiration(new Date(System.currentTimeMillis() + 900_000))
                .signWith(key, Jwts.SIG.HS256).compact();
    }

    private HttpEntity<Map<String, Object>> order(String idemKey, int qty) {
        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(buyerToken());
        h.add("Idempotency-Key", idemKey);
        return new HttpEntity<>(Map.of("items",
                List.of(Map.of("productId", FakeStockServer.WIDGET_ID.toString(), "quantity", qty))), h);
    }

    @Test
    void placeOrderReservesStockAndPublishesEvent() {
        FakeStockServer.STOCK.put(FakeStockServer.WIDGET_ID.toString(), 10);

        ResponseEntity<Map> res = rest.postForEntity("/api/v1/orders", order("idem-A", 3), Map.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(res.getBody().get("status")).isEqualTo("AWAITING_PAYMENT");
        assertThat(((Number) res.getBody().get("total")).doubleValue()).isEqualTo(29.97);
        assertThat(FakeStockServer.STOCK.get(FakeStockServer.WIDGET_ID.toString())).isEqualTo(7);

        // order.placed landed on the stream
        List<MapRecord<String, Object, Object>> recs =
                redisTemplate.opsForStream().range("order.placed", Range.unbounded());
        assertThat(recs).anyMatch(r -> "order.placed".equals(r.getValue().get("eventType")));
    }

    @Test
    void repeatedIdempotencyKeyReturnsSameOrderNoDoubleReserve() {
        FakeStockServer.STOCK.put(FakeStockServer.WIDGET_ID.toString(), 10);

        ResponseEntity<Map> first = rest.postForEntity("/api/v1/orders", order("idem-B", 2), Map.class);
        ResponseEntity<Map> second = rest.postForEntity("/api/v1/orders", order("idem-B", 2), Map.class);

        assertThat(first.getBody().get("id")).isEqualTo(second.getBody().get("id"));
        // Stock decremented once (10 - 2), not twice.
        assertThat(FakeStockServer.STOCK.get(FakeStockServer.WIDGET_ID.toString())).isEqualTo(8);
    }

    @Test
    void insufficientStockReturns409() {
        FakeStockServer.STOCK.put(FakeStockServer.WIDGET_ID.toString(), 1);

        ResponseEntity<Map> res = rest.postForEntity("/api/v1/orders", order("idem-C", 5), Map.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void missingIdempotencyKeyReturns400() {
        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(buyerToken());
        ResponseEntity<Map> res = rest.postForEntity("/api/v1/orders",
                new HttpEntity<>(Map.of("items", List.of(
                        Map.of("productId", FakeStockServer.WIDGET_ID.toString(), "quantity", 1))), h), Map.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void unauthenticatedReturns401() {
        ResponseEntity<Map> res = rest.postForEntity("/api/v1/orders",
                new HttpEntity<>(Map.of("items", List.of())), Map.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}

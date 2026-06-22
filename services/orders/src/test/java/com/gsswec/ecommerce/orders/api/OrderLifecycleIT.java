package com.gsswec.ecommerce.orders.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.gsswec.ecommerce.stock.grpc.ReleaseRequest;
import com.gsswec.ecommerce.stock.grpc.ReleaseResponse;
import com.gsswec.ecommerce.stock.grpc.ReserveRequest;
import com.gsswec.ecommerce.stock.grpc.ReserveResponse;
import com.gsswec.ecommerce.stock.grpc.ReservedLine;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
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
            "grpc.server.port=-1",
            "grpc.server.in-process-name=orders-lifecycle-it",
            "grpc.client.products.address=in-process:orders-lifecycle-it",
            "grpc.client.products.negotiation-type=plaintext"
        })
@Testcontainers
class OrderLifecycleIT {

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

    @TestConfiguration
    static class FakeStockServer {
        static final UUID WIDGET_ID = UUID.randomUUID();

        @GrpcService
        static class Stub extends StockServiceGrpc.StockServiceImplBase {
            @Override
            public void reserve(ReserveRequest req, StreamObserver<ReserveResponse> obs) {
                var builder = ReserveResponse.newBuilder().setReserved(true);
                for (var line : req.getLinesList()) {
                    builder.addLines(ReservedLine.newBuilder()
                            .setProductId(line.getProductId()).setSku("SKU-1").setName("Widget")
                            .setUnitPrice("9.99").setQuantity(line.getQuantity()).build());
                }
                obs.onNext(builder.build());
                obs.onCompleted();
            }

            @Override
            public void release(ReleaseRequest req, StreamObserver<ReleaseResponse> obs) {
                obs.onNext(ReleaseResponse.newBuilder().setReleased(true).build());
                obs.onCompleted();
            }
        }
    }

    @Autowired
    private TestRestTemplate rest;

    private final UUID buyerId = UUID.randomUUID();
    private final UUID otherBuyerId = UUID.randomUUID();

    private String token(UUID subject, String role) {
        SecretKey key = io.jsonwebtoken.security.Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder().subject(subject.toString()).claim("role", role)
                .issuedAt(new Date()).expiration(new Date(System.currentTimeMillis() + 900_000))
                .signWith(key, Jwts.SIG.HS256).compact();
    }

    private HttpHeaders auth(String token) {
        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(token);
        return h;
    }

    @SuppressWarnings("unchecked")
    private String placeOrder(String idemKey) {
        HttpHeaders h = auth(token(buyerId, "BUYER"));
        h.add("Idempotency-Key", idemKey);
        var body = new HttpEntity<>(Map.of("items",
                List.of(Map.of("productId", FakeStockServer.WIDGET_ID.toString(), "quantity", 1))), h);
        ResponseEntity<Map> res = rest.postForEntity("/api/v1/orders", body, Map.class);
        return (String) res.getBody().get("id");
    }

    @Test
    void buyerListsOnlyOwnOrders() {
        placeOrder("life-list-1");

        ResponseEntity<List> mine = rest.exchange("/api/v1/orders", HttpMethod.GET,
                new HttpEntity<>(auth(token(buyerId, "BUYER"))), List.class);
        ResponseEntity<List> others = rest.exchange("/api/v1/orders", HttpMethod.GET,
                new HttpEntity<>(auth(token(otherBuyerId, "BUYER"))), List.class);

        assertThat(mine.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(mine.getBody()).isNotEmpty();
        assertThat(others.getBody()).isEmpty();
    }

    @Test
    void buyerCannotReadAnothersOrder() {
        String orderId = placeOrder("life-owner-1");

        ResponseEntity<Map> res = rest.exchange("/api/v1/orders/" + orderId, HttpMethod.GET,
                new HttpEntity<>(auth(token(otherBuyerId, "BUYER"))), Map.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void adminCanReadAnyOrder() {
        String orderId = placeOrder("life-admin-read-1");

        ResponseEntity<Map> res = rest.exchange("/api/v1/orders/" + orderId, HttpMethod.GET,
                new HttpEntity<>(auth(token(UUID.randomUUID(), "ADMIN"))), Map.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody().get("id")).isEqualTo(orderId);
    }

    @Test
    void cancelAfterPlacementIsRejectedByStateMachine() {
        // Placement transitions the order to AWAITING_PAYMENT synchronously, and the
        // state machine only allows CANCELLED from PENDING — so a placed order can no
        // longer be cancelled. (Whether buyers should be able to cancel an
        // AWAITING_PAYMENT order is a product decision tracked separately.)
        String orderId = placeOrder("life-cancel-1");

        ResponseEntity<Map> res = rest.exchange("/api/v1/orders/" + orderId + "/cancel",
                HttpMethod.DELETE, new HttpEntity<>(auth(token(buyerId, "BUYER"))), Map.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void adminLegalStatusTransitionSucceeds() {
        String orderId = placeOrder("life-admin-status-1");

        // A placed order is AWAITING_PAYMENT; PAID is a legal transition from there.
        ResponseEntity<Map> res = rest.exchange("/api/v1/orders/" + orderId + "/status",
                HttpMethod.PATCH,
                new HttpEntity<>(Map.of("status", "PAID"), auth(token(UUID.randomUUID(), "ADMIN"))),
                Map.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody().get("status")).isEqualTo("PAID");
    }

    @Test
    void adminIllegalStatusTransitionReturns409() {
        String orderId = placeOrder("life-admin-illegal-1");

        // AWAITING_PAYMENT -> CANCELLED is not in the allowed transition set.
        ResponseEntity<Map> res = rest.exchange("/api/v1/orders/" + orderId + "/status",
                HttpMethod.PATCH,
                new HttpEntity<>(Map.of("status", "CANCELLED"), auth(token(UUID.randomUUID(), "ADMIN"))),
                Map.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void buyerForbiddenFromAdminStatusChange() {
        String orderId = placeOrder("life-forbidden-1");

        ResponseEntity<Map> res = rest.exchange("/api/v1/orders/" + orderId + "/status",
                HttpMethod.PATCH,
                new HttpEntity<>(Map.of("status", "CANCELLED"), auth(token(buyerId, "BUYER"))),
                Map.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}

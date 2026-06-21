package com.gsswec.ecommerce.products.grpc;

import static org.assertj.core.api.Assertions.assertThat;

import com.gsswec.ecommerce.products.application.port.out.ProductRepository;
import com.gsswec.ecommerce.products.domain.model.Product;
import com.gsswec.ecommerce.stock.grpc.ReleaseRequest;
import com.gsswec.ecommerce.stock.grpc.ReserveRequest;
import com.gsswec.ecommerce.stock.grpc.ReserveResponse;
import com.gsswec.ecommerce.stock.grpc.ShortfallReason;
import com.gsswec.ecommerce.stock.grpc.StockLine;
import com.gsswec.ecommerce.stock.grpc.StockServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest(properties = "grpc.server.port=9099")
@Testcontainers
class StockGrpcIT {

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
    }

    @Autowired
    private ProductRepository products;

    private ManagedChannel channel;
    private StockServiceGrpc.StockServiceBlockingStub stub;

    @BeforeEach
    void setUp() {
        channel = ManagedChannelBuilder.forAddress("localhost", 9099).usePlaintext().build();
        stub = StockServiceGrpc.newBlockingStub(channel);
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

    private Product seed(String sku, int stock) {
        return products.save(Product.create("Seed " + sku, sku, "desc", null,
                new BigDecimal("9.99"), stock, new BigDecimal("0.1")));
    }

    @Test
    void reserveSucceedsAndDecrementsStock() {
        var p = seed("GRPC-OK", 10);

        ReserveResponse res = stub.reserve(ReserveRequest.newBuilder()
                .setOrderId("order-1")
                .addLines(StockLine.newBuilder().setProductId(p.id().toString()).setSku("GRPC-OK").setQuantity(3))
                .build());

        assertThat(res.getReserved()).isTrue();
        assertThat(products.findBySku("GRPC-OK").orElseThrow().stock()).isEqualTo(7);
        // Reserved-line details come back for the order snapshot (Products is price authority).
        assertThat(res.getLinesList()).hasSize(1);
        assertThat(res.getLines(0).getSku()).isEqualTo("GRPC-OK");
        assertThat(res.getLines(0).getName()).isEqualTo("Seed GRPC-OK");
        assertThat(res.getLines(0).getUnitPrice()).isEqualTo("9.99");
        assertThat(res.getLines(0).getQuantity()).isEqualTo(3);
    }

    @Test
    void reserveFailsWithShortfallWhenInsufficient() {
        seed("GRPC-LOW", 2);

        ReserveResponse res = stub.reserve(ReserveRequest.newBuilder()
                .setOrderId("order-2")
                .addLines(StockLine.newBuilder().setSku("GRPC-LOW").setQuantity(5))
                .build());

        assertThat(res.getReserved()).isFalse();
        assertThat(res.getShortfallsList()).hasSize(1);
        assertThat(res.getShortfalls(0).getReason()).isEqualTo(ShortfallReason.INSUFFICIENT_STOCK);
        // Stock unchanged after a failed (rolled-back) reservation.
        assertThat(products.findBySku("GRPC-LOW").orElseThrow().stock()).isEqualTo(2);
    }

    @Test
    void reserveIsAllOrNothingAcrossLines() {
        seed("GRPC-A", 10);
        seed("GRPC-B", 1);

        ReserveResponse res = stub.reserve(ReserveRequest.newBuilder()
                .setOrderId("order-3")
                .addLines(StockLine.newBuilder().setSku("GRPC-A").setQuantity(5))
                .addLines(StockLine.newBuilder().setSku("GRPC-B").setQuantity(5))
                .build());

        assertThat(res.getReserved()).isFalse();
        // The satisfiable line (A) must be rolled back too — all or nothing.
        assertThat(products.findBySku("GRPC-A").orElseThrow().stock()).isEqualTo(10);
    }

    @Test
    void releaseRestoresStock() {
        seed("GRPC-REL", 5);
        stub.reserve(ReserveRequest.newBuilder().setOrderId("o")
                .addLines(StockLine.newBuilder().setSku("GRPC-REL").setQuantity(3)).build());
        assertThat(products.findBySku("GRPC-REL").orElseThrow().stock()).isEqualTo(2);

        stub.release(ReleaseRequest.newBuilder().setOrderId("o")
                .addLines(StockLine.newBuilder().setSku("GRPC-REL").setQuantity(3)).build());

        assertThat(products.findBySku("GRPC-REL").orElseThrow().stock()).isEqualTo(5);
    }

    @Test
    void reserveUnknownSkuReportsNotFound() {
        ReserveResponse res = stub.reserve(ReserveRequest.newBuilder().setOrderId("o")
                .addLines(StockLine.newBuilder().setSku("NOPE").setQuantity(1)).build());

        assertThat(res.getReserved()).isFalse();
        assertThat(res.getShortfalls(0).getReason()).isEqualTo(ShortfallReason.PRODUCT_NOT_FOUND);
    }
}

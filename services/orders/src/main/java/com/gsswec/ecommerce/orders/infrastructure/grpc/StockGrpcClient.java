package com.gsswec.ecommerce.orders.infrastructure.grpc;

import com.gsswec.ecommerce.orders.application.port.out.StockClient;
import com.gsswec.ecommerce.stock.grpc.ReleaseRequest;
import com.gsswec.ecommerce.stock.grpc.ReserveRequest;
import com.gsswec.ecommerce.stock.grpc.ReserveResponse;
import com.gsswec.ecommerce.stock.grpc.StockLine;
import com.gsswec.ecommerce.stock.grpc.StockServiceGrpc;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.TimeUnit;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;

@Component
public class StockGrpcClient implements StockClient {

    // Deadline caps worst-case thread-hold time on the hot path (ADR-015 / #44).
    private static final long DEADLINE_MS = 2_000;

    @GrpcClient("products")
    private StockServiceGrpc.StockServiceBlockingStub stub;

    @Override
    @CircuitBreaker(name = "stock")
    public ReserveResult reserve(String orderId, List<ReserveLine> lines) {
        ReserveRequest.Builder req = ReserveRequest.newBuilder().setOrderId(orderId);
        for (ReserveLine l : lines) {
            req.addLines(StockLine.newBuilder()
                    .setProductId(l.productId() == null ? "" : l.productId())
                    .setSku(l.sku() == null ? "" : l.sku())
                    .setQuantity(l.quantity()));
        }
        ReserveResponse res = deadlineStub().reserve(req.build());

        List<ReservedLine> reserved = res.getLinesList().stream()
                .map(r -> new ReservedLine(r.getProductId(), r.getSku(), r.getName(),
                        new BigDecimal(r.getUnitPrice()), r.getQuantity()))
                .toList();
        List<Shortfall> shortfalls = res.getShortfallsList().stream()
                .map(s -> new Shortfall(s.getSku(), s.getRequested(), s.getAvailable(), s.getReason().name()))
                .toList();
        return new ReserveResult(res.getReserved(), reserved, shortfalls);
    }

    @Override
    @CircuitBreaker(name = "stock")
    public void release(String orderId, List<ReserveLine> lines) {
        ReleaseRequest.Builder req = ReleaseRequest.newBuilder().setOrderId(orderId);
        for (ReserveLine l : lines) {
            req.addLines(StockLine.newBuilder()
                    .setProductId(l.productId() == null ? "" : l.productId())
                    .setSku(l.sku() == null ? "" : l.sku())
                    .setQuantity(l.quantity()));
        }
        deadlineStub().release(req.build());
    }

    private StockServiceGrpc.StockServiceBlockingStub deadlineStub() {
        return stub.withDeadlineAfter(DEADLINE_MS, TimeUnit.MILLISECONDS);
    }
}

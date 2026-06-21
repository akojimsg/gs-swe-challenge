package com.gsswec.ecommerce.products.application.usecase;

import com.gsswec.ecommerce.products.application.port.out.StockStore;
import com.gsswec.ecommerce.products.domain.model.Product;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReserveStock {

    private final StockStore stock;

    public ReserveStock(StockStore stock) {
        this.stock = stock;
    }

    // Atomic, all-or-nothing reservation. Each line is decremented with a
    // conditional UPDATE (row-locked); the first line that cannot be satisfied
    // aborts the transaction, rolling back any decrements already applied, and the
    // result reports every shortfall. Throwing is avoided — the caller (gRPC) gets
    // a value either way.
    @Transactional
    public Result reserve(List<Line> lines) {
        List<Shortfall> shortfalls = new ArrayList<>();
        List<ReservedLine> reserved = new ArrayList<>();

        for (Line line : lines) {
            Optional<Product> product = stock.findBySku(line.sku());
            if (product.isEmpty()) {
                shortfalls.add(new Shortfall(line, Reason.PRODUCT_NOT_FOUND, 0));
                continue;
            }
            Product p = product.get();
            if (!p.active()) {
                shortfalls.add(new Shortfall(line, Reason.PRODUCT_INACTIVE, p.stock()));
                continue;
            }
            boolean ok = stock.tryDecrement(line.sku(), line.quantity());
            if (ok) {
                // Authoritative details from the same locked read — Orders snapshots these.
                reserved.add(new ReservedLine(
                        p.id().toString(), p.sku(), p.name(), p.price(), line.quantity()));
            } else {
                shortfalls.add(new Shortfall(line, Reason.INSUFFICIENT_STOCK, p.stock()));
            }
        }

        if (!shortfalls.isEmpty()) {
            // Roll back any successful decrements from this attempt.
            throw new ReservationFailed(shortfalls);
        }
        return new Result(true, List.of(), reserved);
    }

    @Transactional
    public void release(List<Line> lines) {
        for (Line line : lines) {
            stock.increment(line.sku(), line.quantity());
        }
    }

    public record Line(String productId, String sku, int quantity) {
    }

    public record Result(boolean reserved, List<Shortfall> shortfalls, List<ReservedLine> lines) {
    }

    public record Shortfall(Line line, Reason reason, int available) {
    }

    public record ReservedLine(String productId, String sku, String name,
            java.math.BigDecimal unitPrice, int quantity) {
    }

    public enum Reason {
        INSUFFICIENT_STOCK, PRODUCT_NOT_FOUND, PRODUCT_INACTIVE
    }

    // Thrown to force transactional rollback of partial reservations; carries the
    // shortfalls so the gRPC layer can translate them into a ReserveResponse.
    public static class ReservationFailed extends RuntimeException {
        private final transient List<Shortfall> shortfalls;

        public ReservationFailed(List<Shortfall> shortfalls) {
            super("Stock reservation failed");
            this.shortfalls = shortfalls;
        }

        public List<Shortfall> shortfalls() {
            return shortfalls;
        }
    }
}

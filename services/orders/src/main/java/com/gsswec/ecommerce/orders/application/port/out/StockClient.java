package com.gsswec.ecommerce.orders.application.port.out;

import java.math.BigDecimal;
import java.util.List;

// Outbound port over the Products gRPC stock server. Keeps generated stubs out of
// the application layer (hexagonal); the adapter maps to/from proto.
public interface StockClient {

    ReserveResult reserve(String orderId, List<ReserveLine> lines);

    void release(String orderId, List<ReserveLine> lines);

    record ReserveLine(String productId, String sku, int quantity) {
    }

    // On success: reservedLines populated, shortfalls empty. On failure: reverse.
    record ReserveResult(boolean reserved, List<ReservedLine> reservedLines, List<Shortfall> shortfalls) {
    }

    record ReservedLine(String productId, String sku, String name, BigDecimal unitPrice, int quantity) {
    }

    record Shortfall(String sku, int requested, int available, String reason) {
    }
}

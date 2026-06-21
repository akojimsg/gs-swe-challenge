package com.gsswec.ecommerce.orders.domain.exception;

import java.util.List;

// Raised when stock reservation fails; carries the unsatisfied lines so the API
// can report which SKUs were short.
public class InsufficientStockException extends RuntimeException {

    private final transient List<Shortfall> shortfalls;

    public InsufficientStockException(List<Shortfall> shortfalls) {
        super("Insufficient stock to place order");
        this.shortfalls = shortfalls;
    }

    public List<Shortfall> shortfalls() {
        return shortfalls;
    }

    public record Shortfall(String sku, int requested, int available, String reason) {
    }
}

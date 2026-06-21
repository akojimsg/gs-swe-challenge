package com.gsswec.ecommerce.orders.domain.model;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItem(
        UUID id,
        UUID productId,
        String sku,
        String name,
        Integer quantity,
        BigDecimal unitPrice) {

    public static OrderItem of(UUID productId, String sku, String name, int quantity, BigDecimal unitPrice) {
        return new OrderItem(null, productId, sku, name, quantity, unitPrice);
    }

    public BigDecimal lineTotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}

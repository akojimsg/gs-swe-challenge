package com.gsswec.ecommerce.orders.api.rest.dto;

import com.gsswec.ecommerce.orders.domain.model.Order;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        UUID userId,
        String status,
        BigDecimal total,
        List<Item> items) {

    public record Item(UUID productId, String sku, String name, Integer quantity, BigDecimal unitPrice) {
    }

    public static OrderResponse from(Order o) {
        return new OrderResponse(o.id(), o.userId(), o.status().name(), o.total(),
                o.items().stream()
                        .map(i -> new Item(i.productId(), i.sku(), i.name(), i.quantity(), i.unitPrice()))
                        .toList());
    }
}

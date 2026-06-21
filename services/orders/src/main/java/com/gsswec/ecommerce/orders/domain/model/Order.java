package com.gsswec.ecommerce.orders.domain.model;

import com.gsswec.ecommerce.orders.domain.exception.IllegalStatusTransitionException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record Order(
        UUID id,
        UUID userId,
        OrderStatus status,
        BigDecimal total,
        String idempotencyKey,
        List<OrderItem> items,
        Instant createdAt,
        Instant updatedAt) {

    public Order {
        items = items == null ? List.of() : List.copyOf(items);
    }

    public static Order place(UUID userId, String idempotencyKey, List<OrderItem> items) {
        BigDecimal total = items.stream()
                .map(OrderItem::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new Order(null, userId, OrderStatus.PENDING, total, idempotencyKey, items, null, null);
    }

    // Move to a new status, enforcing the lifecycle state machine. An illegal
    // transition is rejected here, in the domain — not just at the API edge.
    public Order transitionTo(OrderStatus target) {
        if (!status.canTransitionTo(target)) {
            throw new IllegalStatusTransitionException(status, target);
        }
        return new Order(id, userId, target, total, idempotencyKey, items, createdAt, updatedAt);
    }

    public boolean ownedBy(UUID candidateUserId) {
        return userId.equals(candidateUserId);
    }
}

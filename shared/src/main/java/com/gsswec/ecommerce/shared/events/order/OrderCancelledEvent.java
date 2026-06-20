package com.gsswec.ecommerce.shared.events.order;

import com.gsswec.ecommerce.shared.dto.OrderItemSnapshot;
import com.gsswec.ecommerce.shared.events.base.BaseEvent;
import com.gsswec.ecommerce.shared.events.base.DomainEvent;
import java.util.List;
import java.util.UUID;

public record OrderCancelledEvent(
        BaseEvent base,
        UUID orderId,
        UUID userId,
        String reason,
        List<OrderItemSnapshot> items) implements DomainEvent {

    public OrderCancelledEvent {
        items = items == null ? List.of() : List.copyOf(items);
    }
}

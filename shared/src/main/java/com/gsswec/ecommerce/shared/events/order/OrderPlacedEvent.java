package com.gsswec.ecommerce.shared.events.order;

import com.gsswec.ecommerce.shared.dto.OrderItemSnapshot;
import com.gsswec.ecommerce.shared.events.base.BaseEvent;
import com.gsswec.ecommerce.shared.events.base.DomainEvent;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record OrderPlacedEvent(
        BaseEvent base,
        UUID orderId,
        UUID userId,
        List<OrderItemSnapshot> items,
        BigDecimal total) implements DomainEvent {

    public OrderPlacedEvent {
        items = items == null ? List.of() : List.copyOf(items);
    }
}

package com.gsswec.ecommerce.shared.events.product;

import com.gsswec.ecommerce.shared.events.base.BaseEvent;
import com.gsswec.ecommerce.shared.events.base.DomainEvent;
import java.math.BigDecimal;
import java.util.UUID;

public record ProductCreatedEvent(
        BaseEvent base,
        UUID productId,
        String name,
        String sku,
        BigDecimal price,
        Integer stock,
        String category) implements DomainEvent {
}

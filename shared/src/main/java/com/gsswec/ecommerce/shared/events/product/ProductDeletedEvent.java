package com.gsswec.ecommerce.shared.events.product;

import com.gsswec.ecommerce.shared.events.base.BaseEvent;
import com.gsswec.ecommerce.shared.events.base.DomainEvent;
import java.util.UUID;

public record ProductDeletedEvent(
        BaseEvent base,
        UUID productId,
        String sku) implements DomainEvent {
}

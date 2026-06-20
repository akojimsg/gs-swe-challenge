package com.gsswec.ecommerce.shared.events.product;

import com.gsswec.ecommerce.shared.events.base.BaseEvent;
import com.gsswec.ecommerce.shared.events.base.DomainEvent;
import java.util.UUID;

public record ProductStockLowEvent(
        BaseEvent base,
        UUID productId,
        String sku,
        String name,
        Integer currentStock,
        Integer threshold) implements DomainEvent {
}

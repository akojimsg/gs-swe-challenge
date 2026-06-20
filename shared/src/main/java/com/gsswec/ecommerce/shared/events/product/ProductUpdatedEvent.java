package com.gsswec.ecommerce.shared.events.product;

import com.gsswec.ecommerce.shared.events.base.BaseEvent;
import com.gsswec.ecommerce.shared.events.base.DomainEvent;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public record ProductUpdatedEvent(
        BaseEvent base,
        UUID productId,
        Map<String, Object> changedFields) implements DomainEvent {

    public ProductUpdatedEvent {
        changedFields = changedFields == null
                ? Map.of()
                : Collections.unmodifiableMap(new HashMap<>(changedFields));
    }
}

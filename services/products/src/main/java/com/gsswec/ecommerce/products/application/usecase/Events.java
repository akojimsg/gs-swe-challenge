package com.gsswec.ecommerce.products.application.usecase;

import com.gsswec.ecommerce.shared.events.base.BaseEvent;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

// Small helper for building the BaseEvent envelope from this service.
final class Events {

    private Events() {
    }

    static BaseEvent base(String eventType, Clock clock) {
        return new BaseEvent(UUID.randomUUID(), eventType, "1.0", Instant.now(clock), null, "products");
    }
}

package com.gsswec.ecommerce.shared.events.base;

import java.time.Instant;
import java.util.UUID;

public record BaseEvent(
        UUID eventId,
        String eventType,
        String version,
        Instant timestamp,
        String traceId,
        String producedBy) {
}

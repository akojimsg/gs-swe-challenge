package com.gsswec.ecommerce.notifications.domain.model;

import java.time.Instant;
import java.util.UUID;

public record NotificationLog(
        UUID id,
        UUID eventId,
        String eventType,
        String recipient,
        String template,
        String status,
        String error,
        Instant createdAt) {}

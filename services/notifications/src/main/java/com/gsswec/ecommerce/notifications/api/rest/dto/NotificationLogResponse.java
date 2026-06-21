package com.gsswec.ecommerce.notifications.api.rest.dto;

import java.time.Instant;
import java.util.UUID;

public record NotificationLogResponse(
        UUID id,
        UUID eventId,
        String eventType,
        String recipient,
        String template,
        String status,
        String error,
        Instant createdAt) {}

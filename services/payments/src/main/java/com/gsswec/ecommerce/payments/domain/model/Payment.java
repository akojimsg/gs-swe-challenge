package com.gsswec.ecommerce.payments.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record Payment(
        UUID id,
        UUID orderId,
        UUID userId,
        BigDecimal amount,
        PaymentStatus status,
        String method,
        String last4,
        String failureReason,
        Instant createdAt,
        Instant updatedAt) {

    public static Payment succeeded(UUID orderId, UUID userId, BigDecimal amount, String last4) {
        return new Payment(null, orderId, userId, amount, PaymentStatus.SUCCEEDED,
                "FAKE_CARD", last4, null, null, null);
    }

    public static Payment failed(UUID orderId, UUID userId, BigDecimal amount, String reason) {
        return new Payment(null, orderId, userId, amount, PaymentStatus.FAILED,
                "FAKE_CARD", null, reason, null, null);
    }

    public boolean isSucceeded() {
        return status == PaymentStatus.SUCCEEDED;
    }
}

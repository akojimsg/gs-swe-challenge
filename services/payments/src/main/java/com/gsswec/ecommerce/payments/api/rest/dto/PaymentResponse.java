package com.gsswec.ecommerce.payments.api.rest.dto;

import com.gsswec.ecommerce.payments.domain.model.Payment;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        UUID orderId,
        UUID userId,
        BigDecimal amount,
        String status,
        String method,
        String last4,
        String failureReason,
        Instant createdAt) {

    public static PaymentResponse from(Payment p) {
        return new PaymentResponse(p.id(), p.orderId(), p.userId(), p.amount(),
                p.status().name(), p.method(), p.last4(), p.failureReason(), p.createdAt());
    }
}

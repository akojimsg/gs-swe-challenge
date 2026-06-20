package com.gsswec.ecommerce.shared.events.payment;

import com.gsswec.ecommerce.shared.events.base.BaseEvent;
import com.gsswec.ecommerce.shared.events.base.DomainEvent;
import java.math.BigDecimal;
import java.util.UUID;

public record PaymentRefundedEvent(
        BaseEvent base,
        UUID paymentId,
        UUID orderId,
        UUID userId,
        BigDecimal amount) implements DomainEvent {
}

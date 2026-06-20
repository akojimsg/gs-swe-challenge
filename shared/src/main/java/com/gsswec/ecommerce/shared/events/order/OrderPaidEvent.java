package com.gsswec.ecommerce.shared.events.order;

import com.gsswec.ecommerce.shared.events.base.BaseEvent;
import com.gsswec.ecommerce.shared.events.base.DomainEvent;
import java.math.BigDecimal;
import java.util.UUID;

public record OrderPaidEvent(
        BaseEvent base,
        UUID orderId,
        UUID userId,
        BigDecimal total,
        UUID paymentId) implements DomainEvent {
}

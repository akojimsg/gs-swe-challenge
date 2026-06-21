package com.gsswec.ecommerce.payments.application.usecase;

import com.gsswec.ecommerce.payments.application.port.out.EventPublisher;
import com.gsswec.ecommerce.payments.application.port.out.PaymentProcessor;
import com.gsswec.ecommerce.payments.application.port.out.PaymentRepository;
import com.gsswec.ecommerce.payments.domain.model.Payment;
import com.gsswec.ecommerce.shared.constants.StreamNames;
import com.gsswec.ecommerce.shared.events.base.BaseEvent;
import com.gsswec.ecommerce.shared.events.payment.PaymentFailedEvent;
import com.gsswec.ecommerce.shared.events.payment.PaymentSucceededEvent;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProcessPayment {

    private final PaymentRepository payments;
    private final PaymentProcessor processor;
    private final EventPublisher events;
    private final Clock clock;

    public ProcessPayment(PaymentRepository payments, PaymentProcessor processor,
            EventPublisher events, Clock clock) {
        this.payments = payments;
        this.processor = processor;
        this.events = events;
        this.clock = clock;
    }

    // Saga step: triggered by order.placed. Idempotent on orderId — a redelivered
    // event re-publishes the existing outcome rather than charging twice (the
    // payments.order_id UNIQUE constraint is the hard backstop).
    @Transactional
    public void process(UUID orderId, UUID userId, BigDecimal amount) {
        Optional<Payment> existing = payments.findByOrderId(orderId);
        if (existing.isPresent()) {
            republish(existing.get());
            return;
        }

        PaymentProcessor.Outcome outcome = processor.charge(amount);
        Payment payment = outcome.approved()
                ? payments.save(Payment.succeeded(orderId, userId, amount, outcome.last4()))
                : payments.save(Payment.failed(orderId, userId, amount, outcome.declineReason()));

        publishOutcome(payment);
    }

    private void publishOutcome(Payment p) {
        if (p.isSucceeded()) {
            events.publish(StreamNames.PAYMENT_SUCCEEDED, new PaymentSucceededEvent(
                    base(StreamNames.PAYMENT_SUCCEEDED), p.id(), p.orderId(), p.userId(),
                    p.amount(), p.method(), p.last4()));
        } else {
            events.publish(StreamNames.PAYMENT_FAILED, new PaymentFailedEvent(
                    base(StreamNames.PAYMENT_FAILED), p.id(), p.orderId(), p.userId(),
                    p.amount(), p.failureReason()));
        }
    }

    // Idempotent re-delivery: re-emit the recorded outcome (consumers dedupe on eventId).
    private void republish(Payment p) {
        publishOutcome(p);
    }

    private BaseEvent base(String eventType) {
        return new BaseEvent(UUID.randomUUID(), eventType, "1.0", Instant.now(clock), null, "payments");
    }
}

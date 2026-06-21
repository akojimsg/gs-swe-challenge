package com.gsswec.ecommerce.payments.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gsswec.ecommerce.payments.application.port.out.EventPublisher;
import com.gsswec.ecommerce.payments.application.port.out.PaymentProcessor;
import com.gsswec.ecommerce.payments.application.port.out.PaymentRepository;
import com.gsswec.ecommerce.payments.application.usecase.ProcessPayment;
import com.gsswec.ecommerce.payments.domain.model.Payment;
import com.gsswec.ecommerce.shared.constants.StreamNames;
import com.gsswec.ecommerce.shared.events.base.DomainEvent;
import com.gsswec.ecommerce.shared.events.payment.PaymentFailedEvent;
import com.gsswec.ecommerce.shared.events.payment.PaymentSucceededEvent;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ProcessPaymentTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-06-20T00:00:00Z"), ZoneOffset.UTC);
    private final UUID orderId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    private PaymentRepository payments;
    private PaymentProcessor processor;
    private EventPublisher events;
    private ProcessPayment processPayment;

    @BeforeEach
    void setUp() {
        payments = org.mockito.Mockito.mock(PaymentRepository.class);
        processor = org.mockito.Mockito.mock(PaymentProcessor.class);
        events = org.mockito.Mockito.mock(EventPublisher.class);
        processPayment = new ProcessPayment(payments, processor, events, clock);
        // save echoes back an id, mimicking the JPA insert.
        when(payments.save(any())).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            return new Payment(UUID.randomUUID(), p.orderId(), p.userId(), p.amount(), p.status(),
                    p.method(), p.last4(), p.failureReason(), Instant.now(clock), Instant.now(clock));
        });
    }

    @Test
    void successfulChargePersistsAndPublishesPaymentSucceeded() {
        when(payments.findByOrderId(orderId)).thenReturn(Optional.empty());
        when(processor.charge(any())).thenReturn(PaymentProcessor.Outcome.approved("4242"));

        processPayment.process(orderId, userId, new BigDecimal("19.98"));

        ArgumentCaptor<DomainEvent> ev = ArgumentCaptor.forClass(DomainEvent.class);
        verify(events).publish(eq(StreamNames.PAYMENT_SUCCEEDED), ev.capture());
        PaymentSucceededEvent e = (PaymentSucceededEvent) ev.getValue();
        assertThat(e.orderId()).isEqualTo(orderId);
        assertThat(e.last4()).isEqualTo("4242");
        assertThat(e.amount()).isEqualByComparingTo("19.98");
    }

    @Test
    void declinedChargePersistsAndPublishesPaymentFailed() {
        when(payments.findByOrderId(orderId)).thenReturn(Optional.empty());
        when(processor.charge(any())).thenReturn(PaymentProcessor.Outcome.declined("CARD_DECLINED"));

        processPayment.process(orderId, userId, new BigDecimal("19.98"));

        ArgumentCaptor<DomainEvent> ev = ArgumentCaptor.forClass(DomainEvent.class);
        verify(events).publish(eq(StreamNames.PAYMENT_FAILED), ev.capture());
        PaymentFailedEvent e = (PaymentFailedEvent) ev.getValue();
        assertThat(e.orderId()).isEqualTo(orderId);
        assertThat(e.reason()).isEqualTo("CARD_DECLINED");
    }

    @Test
    void redeliveredOrderDoesNotChargeAgainButRepublishesOutcome() {
        Payment existing = new Payment(UUID.randomUUID(), orderId, userId, new BigDecimal("19.98"),
                com.gsswec.ecommerce.payments.domain.model.PaymentStatus.SUCCEEDED, "FAKE_CARD",
                "4242", null, Instant.now(clock), Instant.now(clock));
        when(payments.findByOrderId(orderId)).thenReturn(Optional.of(existing));

        processPayment.process(orderId, userId, new BigDecimal("19.98"));

        // No new charge, no new row — but the recorded outcome is re-emitted so the
        // saga can recover if the prior publish was lost.
        verify(processor, never()).charge(any());
        verify(payments, never()).save(any());
        verify(events).publish(eq(StreamNames.PAYMENT_SUCCEEDED), any());
    }
}

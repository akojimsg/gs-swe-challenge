package com.gsswec.ecommerce.orders.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gsswec.ecommerce.orders.application.port.out.EventPublisher;
import com.gsswec.ecommerce.orders.application.port.out.OrderRepository;
import com.gsswec.ecommerce.orders.application.port.out.StockClient;
import com.gsswec.ecommerce.orders.application.usecase.HandlePaymentOutcome;
import com.gsswec.ecommerce.orders.domain.model.Order;
import com.gsswec.ecommerce.orders.domain.model.OrderItem;
import com.gsswec.ecommerce.orders.domain.model.OrderStatus;
import com.gsswec.ecommerce.shared.constants.StreamNames;
import com.gsswec.ecommerce.shared.events.base.DomainEvent;
import com.gsswec.ecommerce.shared.events.order.OrderPaidEvent;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class HandlePaymentOutcomeTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-06-20T00:00:00Z"), ZoneOffset.UTC);
    private final UUID orderId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final UUID productId = UUID.randomUUID();
    private final UUID paymentId = UUID.randomUUID();

    private OrderRepository orders;
    private StockClient stock;
    private EventPublisher events;
    private HandlePaymentOutcome handler;

    @BeforeEach
    void setUp() {
        orders = org.mockito.Mockito.mock(OrderRepository.class);
        stock = org.mockito.Mockito.mock(StockClient.class);
        events = org.mockito.Mockito.mock(EventPublisher.class);
        handler = new HandlePaymentOutcome(orders, stock, events, clock);
        when(orders.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private Order awaitingPayment() {
        return new Order(orderId, userId, OrderStatus.AWAITING_PAYMENT, new BigDecimal("19.98"),
                "key-1", List.of(OrderItem.of(productId, "SKU-1", "Widget", 2, new BigDecimal("9.99"))),
                Instant.now(clock), Instant.now(clock));
    }

    @Test
    void paymentSucceededTransitionsToPaidAndPublishesOrderPaid() {
        when(orders.findById(orderId)).thenReturn(Optional.of(awaitingPayment()));

        handler.onPaymentSucceeded(orderId, paymentId);

        ArgumentCaptor<Order> saved = ArgumentCaptor.forClass(Order.class);
        verify(orders).save(saved.capture());
        assertThat(saved.getValue().status()).isEqualTo(OrderStatus.PAID);

        ArgumentCaptor<DomainEvent> ev = ArgumentCaptor.forClass(DomainEvent.class);
        verify(events).publish(eq(StreamNames.ORDER_PAID), ev.capture());
        OrderPaidEvent e = (OrderPaidEvent) ev.getValue();
        assertThat(e.paymentId()).isEqualTo(paymentId);
        // Success path never compensates.
        verify(stock, never()).release(anyString(), any());
    }

    @Test
    void paymentFailedTransitionsToFailedReleasesStockAndPublishesOrderFailed() {
        when(orders.findById(orderId)).thenReturn(Optional.of(awaitingPayment()));

        handler.onPaymentFailed(orderId, "CARD_DECLINED");

        ArgumentCaptor<Order> saved = ArgumentCaptor.forClass(Order.class);
        verify(orders).save(saved.capture());
        assertThat(saved.getValue().status()).isEqualTo(OrderStatus.FAILED);

        // Compensation: stock released for the reserved lines.
        ArgumentCaptor<List<StockClient.ReserveLine>> lines = ArgumentCaptor.forClass(List.class);
        verify(stock).release(eq(orderId.toString()), lines.capture());
        assertThat(lines.getValue()).hasSize(1);
        assertThat(lines.getValue().get(0).quantity()).isEqualTo(2);

        verify(events).publish(eq(StreamNames.ORDER_FAILED), any());
    }

    @Test
    void replayedPaymentSucceededOnPaidOrderIsIgnored() {
        Order paid = new Order(orderId, userId, OrderStatus.PAID, new BigDecimal("19.98"),
                "key-1", List.of(), Instant.now(clock), Instant.now(clock));
        when(orders.findById(orderId)).thenReturn(Optional.of(paid));

        handler.onPaymentSucceeded(orderId, paymentId);

        // State-machine guard: no second transition, no duplicate order.paid.
        verify(orders, never()).save(any());
        verify(events, never()).publish(anyString(), any());
    }

    @Test
    void replayedPaymentFailedDoesNotDoubleReleaseStock() {
        Order failed = new Order(orderId, userId, OrderStatus.FAILED, new BigDecimal("19.98"),
                "key-1", List.of(OrderItem.of(productId, "SKU-1", "Widget", 2, new BigDecimal("9.99"))),
                Instant.now(clock), Instant.now(clock));
        when(orders.findById(orderId)).thenReturn(Optional.of(failed));

        handler.onPaymentFailed(orderId, "CARD_DECLINED");

        verify(orders, never()).save(any());
        verify(stock, never()).release(anyString(), any());
        verify(events, never()).publish(anyString(), any());
    }

    @Test
    void paymentOutcomeForUnknownOrderIsIgnored() {
        when(orders.findById(orderId)).thenReturn(Optional.empty());

        handler.onPaymentSucceeded(orderId, paymentId);
        handler.onPaymentFailed(orderId, "CARD_DECLINED");

        verify(orders, never()).save(any());
        verify(stock, never()).release(anyString(), any());
        verify(events, never()).publish(anyString(), any());
    }
}

package com.gsswec.ecommerce.orders.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gsswec.ecommerce.orders.application.port.out.EventPublisher;
import com.gsswec.ecommerce.orders.application.port.out.OrderRepository;
import com.gsswec.ecommerce.orders.application.usecase.ManageOrder;
import com.gsswec.ecommerce.orders.domain.exception.IllegalStatusTransitionException;
import com.gsswec.ecommerce.orders.domain.exception.OrderNotFoundException;
import com.gsswec.ecommerce.orders.domain.model.Order;
import com.gsswec.ecommerce.orders.domain.model.OrderItem;
import com.gsswec.ecommerce.orders.domain.model.OrderStatus;
import com.gsswec.ecommerce.shared.constants.StreamNames;
import com.gsswec.ecommerce.shared.events.order.OrderCancelledEvent;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

class ManageOrderTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-06-20T00:00:00Z"), ZoneOffset.UTC);
    private final UUID orderId = UUID.randomUUID();
    private final UUID ownerId = UUID.randomUUID();
    private final UUID otherId = UUID.randomUUID();

    private OrderRepository orders;
    private EventPublisher events;
    private ManageOrder manageOrder;

    @BeforeEach
    void setUp() {
        orders = org.mockito.Mockito.mock(OrderRepository.class);
        events = org.mockito.Mockito.mock(EventPublisher.class);
        manageOrder = new ManageOrder(orders, events, clock);
        when(orders.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private Order order(OrderStatus status) {
        return new Order(orderId, ownerId, status, new BigDecimal("19.98"), "key-1",
                List.of(OrderItem.of(UUID.randomUUID(), "SKU-1", "Widget", 2, new BigDecimal("9.99"))),
                Instant.now(clock), Instant.now(clock));
    }

    @Test
    void ownerCancelsPendingOrderAndEventIsPublished() {
        when(orders.findById(orderId)).thenReturn(Optional.of(order(OrderStatus.PENDING)));

        Order result = manageOrder.cancel(orderId, ownerId);

        assertThat(result.status()).isEqualTo(OrderStatus.CANCELLED);
        verify(events).publish(eq(StreamNames.ORDER_CANCELLED), any(OrderCancelledEvent.class));
    }

    @Test
    void nonOwnerCannotCancel() {
        when(orders.findById(orderId)).thenReturn(Optional.of(order(OrderStatus.PENDING)));

        assertThatThrownBy(() -> manageOrder.cancel(orderId, otherId))
                .isInstanceOf(AccessDeniedException.class);

        verify(orders, never()).save(any());
        verify(events, never()).publish(any(), any());
    }

    @Test
    void cannotCancelOnceAwaitingPayment() {
        when(orders.findById(orderId)).thenReturn(Optional.of(order(OrderStatus.AWAITING_PAYMENT)));

        // The state machine only allows CANCELLED from PENDING.
        assertThatThrownBy(() -> manageOrder.cancel(orderId, ownerId))
                .isInstanceOf(IllegalStatusTransitionException.class);
        verify(events, never()).publish(any(), any());
    }

    @Test
    void cancelUnknownOrderThrowsNotFound() {
        when(orders.findById(orderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> manageOrder.cancel(orderId, ownerId))
                .isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    void adminStatusChangeFollowsStateMachine() {
        when(orders.findById(orderId)).thenReturn(Optional.of(order(OrderStatus.PENDING)));

        Order result = manageOrder.changeStatus(orderId, OrderStatus.AWAITING_PAYMENT);

        assertThat(result.status()).isEqualTo(OrderStatus.AWAITING_PAYMENT);
    }

    @Test
    void adminIllegalStatusChangeIsRejected() {
        when(orders.findById(orderId)).thenReturn(Optional.of(order(OrderStatus.PENDING)));

        // PENDING -> PAID is not a legal transition.
        assertThatThrownBy(() -> manageOrder.changeStatus(orderId, OrderStatus.PAID))
                .isInstanceOf(IllegalStatusTransitionException.class);
    }
}

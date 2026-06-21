package com.gsswec.ecommerce.orders.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.gsswec.ecommerce.orders.domain.exception.IllegalStatusTransitionException;
import com.gsswec.ecommerce.orders.domain.model.Order;
import com.gsswec.ecommerce.orders.domain.model.OrderItem;
import com.gsswec.ecommerce.orders.domain.model.OrderStatus;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OrderStatusTest {

    private Order pendingOrder() {
        return Order.place(UUID.randomUUID(), "key-1", List.of(
                OrderItem.of(UUID.randomUUID(), "SKU-1", "Widget", 2, new BigDecimal("9.99"))));
    }

    @Test
    void placeComputesTotalAndStartsPending() {
        Order o = pendingOrder();
        assertThat(o.status()).isEqualTo(OrderStatus.PENDING);
        assertThat(o.total()).isEqualByComparingTo("19.98");
    }

    @Test
    void legalTransitionsAreAllowed() {
        Order o = pendingOrder().transitionTo(OrderStatus.AWAITING_PAYMENT);
        assertThat(o.status()).isEqualTo(OrderStatus.AWAITING_PAYMENT);
        assertThat(o.transitionTo(OrderStatus.PAID).status()).isEqualTo(OrderStatus.PAID);
    }

    @Test
    void pendingCanCancel() {
        assertThat(pendingOrder().transitionTo(OrderStatus.CANCELLED).status())
                .isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void illegalTransitionsAreRejected() {
        // PENDING -> PAID skips AWAITING_PAYMENT
        assertThatThrownBy(() -> pendingOrder().transitionTo(OrderStatus.PAID))
                .isInstanceOf(IllegalStatusTransitionException.class);
        // terminal states cannot move
        Order paid = pendingOrder().transitionTo(OrderStatus.AWAITING_PAYMENT).transitionTo(OrderStatus.PAID);
        assertThatThrownBy(() -> paid.transitionTo(OrderStatus.CANCELLED))
                .isInstanceOf(IllegalStatusTransitionException.class);
        assertThat(paid.status().isTerminal()).isTrue();
    }

    @Test
    void cannotCancelAfterAwaitingPayment() {
        Order awaiting = pendingOrder().transitionTo(OrderStatus.AWAITING_PAYMENT);
        assertThatThrownBy(() -> awaiting.transitionTo(OrderStatus.CANCELLED))
                .isInstanceOf(IllegalStatusTransitionException.class);
    }
}

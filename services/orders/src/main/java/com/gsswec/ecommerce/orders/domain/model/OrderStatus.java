package com.gsswec.ecommerce.orders.domain.model;

import java.util.Set;

public enum OrderStatus {
    PENDING,
    AWAITING_PAYMENT,
    PAID,
    FAILED,
    CANCELLED;

    // Legal transitions (the order lifecycle state machine). Encoded here in the
    // domain so an illegal transition is rejected by the model, not just the API.
    private static final java.util.Map<OrderStatus, Set<OrderStatus>> TRANSITIONS = java.util.Map.of(
            PENDING, Set.of(AWAITING_PAYMENT, CANCELLED),
            AWAITING_PAYMENT, Set.of(PAID, FAILED),
            PAID, Set.of(),
            FAILED, Set.of(),
            CANCELLED, Set.of());

    public boolean canTransitionTo(OrderStatus target) {
        return TRANSITIONS.get(this).contains(target);
    }

    public boolean isTerminal() {
        return TRANSITIONS.get(this).isEmpty();
    }
}

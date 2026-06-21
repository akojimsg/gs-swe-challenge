package com.gsswec.ecommerce.orders.domain.exception;

import com.gsswec.ecommerce.orders.domain.model.OrderStatus;

public class IllegalStatusTransitionException extends RuntimeException {

    public IllegalStatusTransitionException(OrderStatus from, OrderStatus to) {
        super("Illegal order status transition: " + from + " -> " + to);
    }
}

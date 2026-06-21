package com.gsswec.ecommerce.orders.domain.exception;

public class OrderNotFoundException extends RuntimeException {

    public OrderNotFoundException() {
        super("Order not found");
    }
}

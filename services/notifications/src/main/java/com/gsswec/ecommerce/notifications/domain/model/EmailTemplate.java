package com.gsswec.ecommerce.notifications.domain.model;

// Maps each triggering event type to its email template name.
// Subject lines are kept here so template selection stays in the domain.
public enum EmailTemplate {

    WELCOME("welcome", "Welcome to United Deals!"),
    ROLE_CHANGED("role_changed", "Your account role has been updated"),
    PRODUCT_CREATED("product_created", "New product listed"),
    STOCK_LOW("stock_low", "Low stock alert"),
    PRODUCT_IMPORTED("product_imported", "CSV import complete"),
    ORDER_PAID("order_paid", "Your order is confirmed — payment received"),
    ORDER_FAILED("order_failed", "Order could not be completed"),
    PAYMENT_SUCCEEDED("payment_succeeded", "Payment successful"),
    PAYMENT_FAILED("payment_failed", "Payment failed");

    public final String name;
    public final String subject;

    EmailTemplate(String name, String subject) {
        this.name = name;
        this.subject = subject;
    }
}

package com.gsswec.ecommerce.shared.constants;

public final class StreamNames {

    private StreamNames() {
    }

    public static final String USER_REGISTERED = "user.registered";
    public static final String USER_ROLE_CHANGED = "user.role_changed";

    public static final String PRODUCT_CREATED = "product.created";
    public static final String PRODUCT_UPDATED = "product.updated";
    public static final String PRODUCT_DELETED = "product.deleted";
    public static final String PRODUCT_STOCK_DEPLETED = "product.stock_depleted";
    public static final String PRODUCT_STOCK_LOW = "product.stock_low";
    public static final String PRODUCT_IMPORTED = "product.imported";

    public static final String ORDER_PLACED = "order.placed";
    public static final String ORDER_PAID = "order.paid";
    public static final String ORDER_FAILED = "order.failed";
    public static final String ORDER_CANCELLED = "order.cancelled";

    public static final String PAYMENT_SUCCEEDED = "payment.succeeded";
    public static final String PAYMENT_FAILED = "payment.failed";
    public static final String PAYMENT_REFUNDED = "payment.refunded";
}

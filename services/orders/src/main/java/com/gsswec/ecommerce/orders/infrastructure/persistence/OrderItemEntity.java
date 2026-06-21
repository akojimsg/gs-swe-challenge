package com.gsswec.ecommerce.orders.infrastructure.persistence;

import com.gsswec.ecommerce.orders.domain.model.OrderItem;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "order_items", schema = "orders_schema")
public class OrderItemEntity {

    @Id
    private UUID id;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(nullable = false)
    private String sku;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "unit_price", nullable = false)
    private BigDecimal unitPrice;

    protected OrderItemEntity() {
    }

    static OrderItemEntity fromDomain(OrderItem i, UUID orderId) {
        OrderItemEntity e = new OrderItemEntity();
        e.id = i.id() == null ? UUID.randomUUID() : i.id();
        e.orderId = orderId;
        e.productId = i.productId();
        e.sku = i.sku();
        e.name = i.name();
        e.quantity = i.quantity();
        e.unitPrice = i.unitPrice();
        return e;
    }

    OrderItem toDomain() {
        return new OrderItem(id, productId, sku, name, quantity, unitPrice);
    }
}

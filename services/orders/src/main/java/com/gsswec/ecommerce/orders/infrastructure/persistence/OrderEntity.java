package com.gsswec.ecommerce.orders.infrastructure.persistence;

import com.gsswec.ecommerce.orders.domain.model.Order;
import com.gsswec.ecommerce.orders.domain.model.OrderStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders", schema = "orders_schema")
public class OrderEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(nullable = false)
    private BigDecimal total;

    @Column(name = "idempotency_key", nullable = false, unique = true)
    private String idempotencyKey;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    @JoinColumn(name = "order_id")
    private List<OrderItemEntity> items = new ArrayList<>();

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private Instant updatedAt;

    protected OrderEntity() {
    }

    static OrderEntity fromDomain(Order o) {
        OrderEntity e = new OrderEntity();
        e.id = o.id() == null ? UUID.randomUUID() : o.id();
        e.userId = o.userId();
        e.status = o.status();
        e.total = o.total();
        e.idempotencyKey = o.idempotencyKey();
        UUID oid = e.id;
        e.items = o.items().stream().map(i -> OrderItemEntity.fromDomain(i, oid)).collect(
                java.util.stream.Collectors.toCollection(ArrayList::new));
        return e;
    }

    Order toDomain() {
        return new Order(id, userId, status, total, idempotencyKey,
                items.stream().map(OrderItemEntity::toDomain).toList(), createdAt, updatedAt);
    }
}

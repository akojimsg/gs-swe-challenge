package com.gsswec.ecommerce.orders.infrastructure.persistence;

import com.gsswec.ecommerce.orders.application.port.out.OrderRepository;
import com.gsswec.ecommerce.orders.domain.model.Order;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class OrderRepositoryAdapter implements OrderRepository {

    private final OrderJpaRepository jpa;

    public OrderRepositoryAdapter(OrderJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Order save(Order order) {
        return jpa.save(OrderEntity.fromDomain(order)).toDomain();
    }

    @Override
    public Optional<Order> findById(UUID id) {
        return jpa.findById(id).map(OrderEntity::toDomain);
    }

    @Override
    public Optional<Order> findByIdempotencyKey(String idempotencyKey) {
        return jpa.findByIdempotencyKey(idempotencyKey).map(OrderEntity::toDomain);
    }

    @Override
    public List<Order> findByUserId(UUID userId) {
        return jpa.findByUserIdOrderByCreatedAtDesc(userId).stream().map(OrderEntity::toDomain).toList();
    }

    @Override
    public List<Order> findAll() {
        return jpa.findAll().stream().map(OrderEntity::toDomain).toList();
    }
}

package com.gsswec.ecommerce.orders.application.port.out;

import com.gsswec.ecommerce.orders.domain.model.Order;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository {

    Order save(Order order);

    Optional<Order> findById(UUID id);

    Optional<Order> findByIdempotencyKey(String idempotencyKey);

    List<Order> findByUserId(UUID userId);

    List<Order> findAll();
}

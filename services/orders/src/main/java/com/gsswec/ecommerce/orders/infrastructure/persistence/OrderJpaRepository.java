package com.gsswec.ecommerce.orders.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderJpaRepository extends JpaRepository<OrderEntity, UUID> {

    Optional<OrderEntity> findByIdempotencyKey(String idempotencyKey);

    List<OrderEntity> findByUserIdOrderByCreatedAtDesc(UUID userId);
}

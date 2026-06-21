package com.gsswec.ecommerce.payments.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentJpaRepository extends JpaRepository<PaymentEntity, UUID> {

    Optional<PaymentEntity> findByOrderId(UUID orderId);

    List<PaymentEntity> findByUserIdOrderByCreatedAtDesc(UUID userId);
}

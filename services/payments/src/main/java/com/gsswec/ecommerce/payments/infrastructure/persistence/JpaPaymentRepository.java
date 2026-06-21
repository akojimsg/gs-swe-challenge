package com.gsswec.ecommerce.payments.infrastructure.persistence;

import com.gsswec.ecommerce.payments.application.port.out.PaymentRepository;
import com.gsswec.ecommerce.payments.domain.model.Payment;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

// Adapter: bridges the domain PaymentRepository port to Spring Data JPA.
@Component
public class JpaPaymentRepository implements PaymentRepository {

    private final PaymentJpaRepository jpa;

    public JpaPaymentRepository(PaymentJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Payment save(Payment payment) {
        return jpa.save(PaymentEntity.fromDomain(payment)).toDomain();
    }

    @Override
    public Optional<Payment> findById(UUID id) {
        return jpa.findById(id).map(PaymentEntity::toDomain);
    }

    @Override
    public Optional<Payment> findByOrderId(UUID orderId) {
        return jpa.findByOrderId(orderId).map(PaymentEntity::toDomain);
    }

    @Override
    public List<Payment> findByUserId(UUID userId) {
        return jpa.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(PaymentEntity::toDomain).toList();
    }

    @Override
    public List<Payment> findAll() {
        return jpa.findAll().stream().map(PaymentEntity::toDomain).toList();
    }
}

package com.gsswec.ecommerce.payments.infrastructure.persistence;

import com.gsswec.ecommerce.payments.domain.model.Payment;
import com.gsswec.ecommerce.payments.domain.model.PaymentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payments", schema = "payments_schema")
public class PaymentEntity {

    @Id
    private UUID id;

    @Column(name = "order_id", nullable = false, unique = true)
    private UUID orderId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @Column(nullable = false)
    private String method;

    private String last4;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private Instant updatedAt;

    protected PaymentEntity() {
    }

    static PaymentEntity fromDomain(Payment p) {
        PaymentEntity e = new PaymentEntity();
        e.id = p.id() == null ? UUID.randomUUID() : p.id();
        e.orderId = p.orderId();
        e.userId = p.userId();
        e.amount = p.amount();
        e.status = p.status();
        e.method = p.method();
        e.last4 = p.last4();
        e.failureReason = p.failureReason();
        return e;
    }

    Payment toDomain() {
        return new Payment(id, orderId, userId, amount, status, method, last4, failureReason,
                createdAt, updatedAt);
    }
}

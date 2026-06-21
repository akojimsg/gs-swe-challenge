package com.gsswec.ecommerce.payments.application.usecase;

import com.gsswec.ecommerce.payments.application.port.out.PaymentRepository;
import com.gsswec.ecommerce.payments.domain.model.Payment;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

// Read-side queries for the payment API. Authorization (owner vs ADMIN) is enforced
// at the controller boundary; this layer is pure data access.
@Service
public class GetPayments {

    private final PaymentRepository payments;

    public GetPayments(PaymentRepository payments) {
        this.payments = payments;
    }

    public Optional<Payment> byId(UUID id) {
        return payments.findById(id);
    }

    public Optional<Payment> byOrderId(UUID orderId) {
        return payments.findByOrderId(orderId);
    }

    public List<Payment> forUser(UUID userId) {
        return payments.findByUserId(userId);
    }

    public List<Payment> all() {
        return payments.findAll();
    }
}

package com.gsswec.ecommerce.payments.api.rest;

import com.gsswec.ecommerce.payments.api.rest.dto.PaymentResponse;
import com.gsswec.ecommerce.payments.application.usecase.GetPayments;
import com.gsswec.ecommerce.payments.domain.model.Payment;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/payments")
@Tag(name = "Payments", description = "Read access to saga-driven payments")
@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")
public class PaymentController {

    private final GetPayments getPayments;

    public PaymentController(GetPayments getPayments) {
        this.getPayments = getPayments;
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get a payment by id (owner or ADMIN)")
    public PaymentResponse byId(@PathVariable UUID id, Authentication auth) {
        Payment payment = getPayments.byId(id)
                .orElseThrow(() -> new ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Payment not found"));
        requireOwnerOrAdmin(payment, auth);
        return PaymentResponse.from(payment);
    }

    @GetMapping("/order/{orderId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get the payment for an order (owner or ADMIN)")
    public PaymentResponse byOrderId(@PathVariable UUID orderId, Authentication auth) {
        Payment payment = getPayments.byOrderId(orderId)
                .orElseThrow(() -> new ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Payment not found"));
        requireOwnerOrAdmin(payment, auth);
        return PaymentResponse.from(payment);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List all payments [ADMIN]")
    public List<PaymentResponse> all() {
        return getPayments.all().stream().map(PaymentResponse::from).toList();
    }

    // A buyer may only read their own payments; ADMIN sees everything. The check is
    // here (not @PreAuthorize) because it depends on the loaded payment's userId.
    private void requireOwnerOrAdmin(Payment payment, Authentication auth) {
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (isAdmin) {
            return;
        }
        UUID caller = UUID.fromString(auth.getName());
        if (!caller.equals(payment.userId())) {
            throw new AccessDeniedException("Not the owner of this payment");
        }
    }
}

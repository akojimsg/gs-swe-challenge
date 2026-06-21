package com.gsswec.ecommerce.payments.application.port.out;

import java.math.BigDecimal;

// The (fake) payment gateway. An outbound port so the 90/10 outcome is injectable
// and tests can force success/failure deterministically.
public interface PaymentProcessor {

    Outcome charge(BigDecimal amount);

    record Outcome(boolean approved, String last4, String declineReason) {
        public static Outcome approved(String last4) {
            return new Outcome(true, last4, null);
        }

        public static Outcome declined(String reason) {
            return new Outcome(false, null, reason);
        }
    }
}

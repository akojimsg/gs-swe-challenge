package com.gsswec.ecommerce.payments.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.gsswec.ecommerce.payments.application.port.out.PaymentProcessor;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class FakePaymentProcessorTest {

    // The RNG is injectable; draws below the success rate approve, at/above decline.
    @Test
    void drawBelowSuccessRateApproves() {
        var processor = new FakePaymentProcessor(0.9, () -> 0.5);
        PaymentProcessor.Outcome outcome = processor.charge(new BigDecimal("10.00"));
        assertThat(outcome.approved()).isTrue();
        assertThat(outcome.last4()).hasSize(4);
    }

    @Test
    void drawAtOrAboveSuccessRateDeclines() {
        var processor = new FakePaymentProcessor(0.9, () -> 0.95);
        PaymentProcessor.Outcome outcome = processor.charge(new BigDecimal("10.00"));
        assertThat(outcome.approved()).isFalse();
        assertThat(outcome.declineReason()).isEqualTo("CARD_DECLINED");
    }
}

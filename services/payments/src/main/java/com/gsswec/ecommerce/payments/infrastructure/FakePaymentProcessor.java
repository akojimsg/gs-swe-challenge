package com.gsswec.ecommerce.payments.infrastructure;

import com.gsswec.ecommerce.payments.application.port.out.PaymentProcessor;
import com.gsswec.ecommerce.payments.infrastructure.config.FakePaymentProperties;
import java.math.BigDecimal;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.DoubleSupplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

// Deliberately fake gateway: approves at the configured success rate (default 90%),
// declines otherwise. The RNG is injectable so tests force a deterministic outcome.
@Component
public class FakePaymentProcessor implements PaymentProcessor {

    private final double successRate;
    private final DoubleSupplier rng;

    @Autowired
    public FakePaymentProcessor(FakePaymentProperties properties) {
        this(properties.successRate(), () -> ThreadLocalRandom.current().nextDouble());
    }

    FakePaymentProcessor(double successRate, DoubleSupplier rng) {
        this.successRate = successRate;
        this.rng = rng;
    }

    @Override
    public Outcome charge(BigDecimal amount) {
        if (rng.getAsDouble() < successRate) {
            return Outcome.approved(randomLast4());
        }
        return Outcome.declined("CARD_DECLINED");
    }

    private static String randomLast4() {
        return String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
    }
}

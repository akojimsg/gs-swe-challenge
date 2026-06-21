package com.gsswec.ecommerce.payments.application.port.out;

import java.util.UUID;

// Idempotency ledger for inbound events. The saga must tolerate Redis Streams
// at-least-once delivery, so every consumed eventId is checked before processing and
// recorded only AFTER it is handled successfully. Marking after (not before) success
// means a transient failure is retried on redelivery instead of being permanently
// suppressed.
public interface ProcessedEventStore {

    // True if this eventId was already handled successfully and should be skipped.
    boolean isProcessed(UUID eventId);

    // Record that this eventId has now been handled successfully.
    void markProcessed(UUID eventId);
}

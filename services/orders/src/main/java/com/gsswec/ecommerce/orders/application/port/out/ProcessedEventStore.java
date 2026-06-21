package com.gsswec.ecommerce.orders.application.port.out;

import java.util.UUID;

// Idempotency ledger for inbound saga events. Redis Streams delivers at-least-once,
// so every consumed eventId is checked before processing and recorded only AFTER it
// is handled successfully. Marking after (not before) success means a transient
// failure or a not-yet-ready order is retried on redelivery instead of being
// permanently suppressed.
public interface ProcessedEventStore {

    // True if this eventId was already handled successfully and should be skipped.
    boolean isProcessed(UUID eventId);

    // Record that this eventId has now been handled successfully.
    void markProcessed(UUID eventId);
}

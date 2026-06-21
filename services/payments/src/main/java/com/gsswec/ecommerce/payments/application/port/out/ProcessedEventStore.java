package com.gsswec.ecommerce.payments.application.port.out;

import java.util.UUID;

// Idempotency ledger for inbound events. The saga must tolerate Redis Streams
// at-least-once delivery, so every consumed eventId is recorded and re-checked.
public interface ProcessedEventStore {

    // True if this is the first time we've seen the event (and it was just recorded);
    // false if it was already processed and should be skipped.
    boolean markIfNew(UUID eventId);
}

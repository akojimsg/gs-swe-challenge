package com.gsswec.ecommerce.orders.application.port.out;

import java.util.UUID;

// Idempotency ledger for inbound saga events. Redis Streams delivers at-least-once,
// so every consumed eventId is recorded and re-checked before any state change.
public interface ProcessedEventStore {

    // True if this is the first time we've seen the event (and it was just recorded);
    // false if it was already processed and should be skipped.
    boolean markIfNew(UUID eventId);
}

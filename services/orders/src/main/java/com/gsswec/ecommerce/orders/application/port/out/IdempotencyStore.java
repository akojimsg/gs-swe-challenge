package com.gsswec.ecommerce.orders.application.port.out;

import java.util.Optional;
import java.util.UUID;

// Redis-backed idempotency for order placement (TTL 24h). Maps an Idempotency-Key
// to the order id created for it, so a retried submission returns the original.
public interface IdempotencyStore {

    Optional<UUID> get(String idempotencyKey);

    void put(String idempotencyKey, UUID orderId);
}

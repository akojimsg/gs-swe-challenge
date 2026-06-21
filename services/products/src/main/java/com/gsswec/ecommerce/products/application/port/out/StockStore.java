package com.gsswec.ecommerce.products.application.port.out;

import com.gsswec.ecommerce.products.domain.model.Product;
import java.util.Optional;
import java.util.UUID;

// Outbound port for the stock hot path used by the gRPC server. Keyed by product
// id (the normalized identity; SKU is a mutable business code). Kept separate from
// the catalogue ProductRepository so reservation concerns are explicit.
public interface StockStore {

    Optional<Product> findById(UUID productId);

    // Atomic conditional decrement; true only if sufficient active stock existed.
    boolean tryDecrement(UUID productId, int quantity);

    // Compensating increment (saga release).
    void increment(UUID productId, int quantity);
}

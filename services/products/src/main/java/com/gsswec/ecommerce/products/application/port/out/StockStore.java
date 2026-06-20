package com.gsswec.ecommerce.products.application.port.out;

import com.gsswec.ecommerce.products.domain.model.Product;
import java.util.Optional;

// Outbound port for the stock hot path used by the gRPC server. Kept separate
// from the catalogue ProductRepository so reservation concerns are explicit.
public interface StockStore {

    Optional<Product> findBySku(String sku);

    // Atomic conditional decrement; true only if sufficient active stock existed.
    boolean tryDecrement(String sku, int quantity);

    // Compensating increment (saga release).
    void increment(String sku, int quantity);
}

package com.gsswec.ecommerce.products.infrastructure.persistence;

import com.gsswec.ecommerce.products.application.port.out.StockStore;
import com.gsswec.ecommerce.products.domain.model.Product;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class StockStoreAdapter implements StockStore {

    private final ProductJpaRepository jpa;

    public StockStoreAdapter(ProductJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<Product> findById(UUID productId) {
        return jpa.findById(productId).map(ProductEntity::toDomain);
    }

    @Override
    public boolean tryDecrement(UUID productId, int quantity) {
        return jpa.tryDecrement(productId, quantity) == 1;
    }

    @Override
    public void increment(UUID productId, int quantity) {
        jpa.increment(productId, quantity);
    }
}

package com.gsswec.ecommerce.products.infrastructure.persistence;

import com.gsswec.ecommerce.products.application.port.out.StockStore;
import com.gsswec.ecommerce.products.domain.model.Product;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class StockStoreAdapter implements StockStore {

    private final ProductJpaRepository jpa;

    public StockStoreAdapter(ProductJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<Product> findBySku(String sku) {
        return jpa.findBySku(sku).map(ProductEntity::toDomain);
    }

    @Override
    public boolean tryDecrement(String sku, int quantity) {
        return jpa.tryDecrement(sku, quantity) == 1;
    }

    @Override
    public void increment(String sku, int quantity) {
        jpa.increment(sku, quantity);
    }
}

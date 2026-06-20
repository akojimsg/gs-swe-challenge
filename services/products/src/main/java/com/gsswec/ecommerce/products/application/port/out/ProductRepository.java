package com.gsswec.ecommerce.products.application.port.out;

import com.gsswec.ecommerce.products.domain.model.Product;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository {

    Product save(Product product);

    Optional<Product> findById(UUID id);

    Optional<Product> findBySku(String sku);

    boolean existsBySku(String sku);

    void deleteById(UUID id);

    Page<Product> search(ProductSearchCriteria criteria);
}

package com.gsswec.ecommerce.products.application.port.out;

import com.gsswec.ecommerce.products.domain.model.Product;
import java.util.Optional;

public interface ProductCache {

    Optional<Page<Product>> getSearch(ProductSearchCriteria criteria);

    void putSearch(ProductSearchCriteria criteria, Page<Product> result);

    // Invalidate all cached search/list results (called on any write).
    void evictAll();
}

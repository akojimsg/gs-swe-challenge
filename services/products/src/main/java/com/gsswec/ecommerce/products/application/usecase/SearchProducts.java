package com.gsswec.ecommerce.products.application.usecase;

import com.gsswec.ecommerce.products.application.port.out.Page;
import com.gsswec.ecommerce.products.application.port.out.ProductCache;
import com.gsswec.ecommerce.products.application.port.out.ProductRepository;
import com.gsswec.ecommerce.products.application.port.out.ProductSearchCriteria;
import com.gsswec.ecommerce.products.domain.model.Product;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SearchProducts {

    private final ProductRepository products;
    private final ProductCache cache;

    public SearchProducts(ProductRepository products, ProductCache cache) {
        this.products = products;
        this.cache = cache;
    }

    @Transactional(readOnly = true)
    public Page<Product> search(ProductSearchCriteria criteria) {
        // CQRS read side (ADR-011): serve presentation data from the cache,
        // falling through to PostgreSQL on miss. Stock is part of the product
        // record but the cache TTL (60s) bounds its staleness; reservation never
        // reads from here — it reads live (see Orders / ADR-011).
        return cache.getSearch(criteria).orElseGet(() -> {
            Page<Product> result = products.search(criteria);
            cache.putSearch(criteria, result);
            return result;
        });
    }
}

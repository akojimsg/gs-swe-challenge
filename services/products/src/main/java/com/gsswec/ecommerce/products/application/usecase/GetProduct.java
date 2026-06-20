package com.gsswec.ecommerce.products.application.usecase;

import com.gsswec.ecommerce.products.application.port.out.ProductRepository;
import com.gsswec.ecommerce.products.domain.exception.ProductNotFoundException;
import com.gsswec.ecommerce.products.domain.model.Product;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetProduct {

    private final ProductRepository products;

    public GetProduct(ProductRepository products) {
        this.products = products;
    }

    @Transactional(readOnly = true)
    public Product byId(UUID id) {
        // Public reads must not expose inactive products.
        return products.findById(id)
                .filter(Product::active)
                .orElseThrow(ProductNotFoundException::new);
    }
}

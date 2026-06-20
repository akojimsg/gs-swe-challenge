package com.gsswec.ecommerce.products.api.rest.dto;

import com.gsswec.ecommerce.products.domain.model.Product;
import java.math.BigDecimal;
import java.util.UUID;

public record ProductResponse(
        UUID id,
        String name,
        String sku,
        String description,
        Integer categoryId,
        BigDecimal price,
        Integer stock,
        BigDecimal weightKg,
        boolean active) {

    public static ProductResponse from(Product p) {
        return new ProductResponse(p.id(), p.name(), p.sku(), p.description(),
                p.categoryId(), p.price(), p.stock(), p.weightKg(), p.active());
    }
}

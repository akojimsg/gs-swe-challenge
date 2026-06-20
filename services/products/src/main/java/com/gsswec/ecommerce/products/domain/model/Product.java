package com.gsswec.ecommerce.products.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record Product(
        UUID id,
        String name,
        String sku,
        String description,
        Integer categoryId,
        BigDecimal price,
        Integer stock,
        BigDecimal weightKg,
        boolean active,
        Instant createdAt,
        Instant updatedAt) {

    public Product {
        if (price != null && price.signum() < 0) {
            throw new IllegalArgumentException("price must be >= 0");
        }
        if (stock != null && stock < 0) {
            throw new IllegalArgumentException("stock must be >= 0");
        }
    }

    public static Product create(
            String name, String sku, String description, Integer categoryId,
            BigDecimal price, Integer stock, BigDecimal weightKg) {
        return new Product(null, name, sku, description, categoryId,
                price, stock == null ? 0 : stock, weightKg, true, null, null);
    }

    public Product withUpdates(
            String newName, String newDescription, Integer newCategoryId,
            BigDecimal newPrice, Integer newStock, BigDecimal newWeightKg, Boolean newActive) {
        return new Product(
                id, newName, sku, newDescription, newCategoryId,
                newPrice, newStock, newWeightKg,
                newActive == null ? active : newActive,
                createdAt, updatedAt);
    }
}

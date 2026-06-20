package com.gsswec.ecommerce.products.application.port.out;

import java.math.BigDecimal;

public record ProductSearchCriteria(
        String query,
        String category,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        Boolean inStock,
        int page,
        int size,
        String sort) {
}

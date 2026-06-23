package com.gsswec.ecommerce.products.api.rest.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

// Shared by PUT (full replace) and PATCH (partial). Fields are individually
// optional at the bean-validation level; PUT semantics are enforced by the
// controller passing partial=false to the use case.
public record UpdateProductRequest(
        @Size(max = 255) String name,
        String description,
        String category,
        @DecimalMin("0.0") BigDecimal price,
        @Min(0) Integer stock,
        BigDecimal weightKg,
        String imageUrl,
        Boolean active) {
}

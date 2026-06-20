package com.gsswec.ecommerce.shared.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemSnapshot(
        UUID productId,
        String sku,
        String name,
        Integer quantity,
        BigDecimal unitPrice) {
}

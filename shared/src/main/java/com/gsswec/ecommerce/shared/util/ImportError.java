package com.gsswec.ecommerce.shared.util;

public record ImportError(
        Integer row,
        String field,
        String value,
        String reason) {
}

package com.gsswec.ecommerce.users.application.port.out;

import java.util.List;

public record Page<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {

    public Page {
        content = content == null ? List.of() : List.copyOf(content);
    }
}

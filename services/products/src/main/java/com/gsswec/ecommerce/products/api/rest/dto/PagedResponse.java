package com.gsswec.ecommerce.products.api.rest.dto;

import com.gsswec.ecommerce.products.application.port.out.Page;
import java.util.List;
import java.util.function.Function;

public record PagedResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {

    public static <D, T> PagedResponse<T> from(Page<D> page, Function<D, T> mapper) {
        return new PagedResponse<>(
                page.content().stream().map(mapper).toList(),
                page.page(), page.size(), page.totalElements(), page.totalPages());
    }
}

package com.gsswec.ecommerce.products.api.rest.dto;

import com.gsswec.ecommerce.products.domain.model.Category;

public record CategoryResponse(Integer id, String name) {

    public static CategoryResponse from(Category c) {
        return new CategoryResponse(c.id(), c.name());
    }
}

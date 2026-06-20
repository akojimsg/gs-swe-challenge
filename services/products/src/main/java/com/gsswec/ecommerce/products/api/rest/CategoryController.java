package com.gsswec.ecommerce.products.api.rest;

import com.gsswec.ecommerce.products.api.rest.dto.CategoryResponse;
import com.gsswec.ecommerce.products.application.usecase.ListCategories;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/categories")
@Tag(name = "Categories", description = "Public category listing")
public class CategoryController {

    private final ListCategories listCategories;

    public CategoryController(ListCategories listCategories) {
        this.listCategories = listCategories;
    }

    @GetMapping
    @Operation(summary = "List all categories (public)")
    public List<CategoryResponse> list() {
        return listCategories.list().stream().map(CategoryResponse::from).toList();
    }
}

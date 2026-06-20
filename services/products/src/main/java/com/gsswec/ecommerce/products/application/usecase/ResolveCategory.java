package com.gsswec.ecommerce.products.application.usecase;

import com.gsswec.ecommerce.products.application.port.out.CategoryRepository;
import com.gsswec.ecommerce.products.domain.model.Category;
import org.springframework.stereotype.Service;

@Service
public class ResolveCategory {

    private final CategoryRepository categories;

    public ResolveCategory(CategoryRepository categories) {
        this.categories = categories;
    }

    // Resolve a category name to its id, creating the category if it does not
    // exist. Blank/null names resolve to null (a product with no category).
    public Integer toIdOrNull(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        String trimmed = name.trim();
        return categories.findByName(trimmed)
                .orElseGet(() -> categories.save(new Category(null, trimmed)))
                .id();
    }
}

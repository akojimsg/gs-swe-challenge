package com.gsswec.ecommerce.products.application.usecase;

import com.gsswec.ecommerce.products.application.port.out.CategoryRepository;
import com.gsswec.ecommerce.products.domain.model.Category;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListCategories {

    private final CategoryRepository categories;

    public ListCategories(CategoryRepository categories) {
        this.categories = categories;
    }

    @Transactional(readOnly = true)
    public List<Category> list() {
        return categories.findAll();
    }
}

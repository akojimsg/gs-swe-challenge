package com.gsswec.ecommerce.products.infrastructure.persistence;

import com.gsswec.ecommerce.products.application.port.out.CategoryRepository;
import com.gsswec.ecommerce.products.domain.model.Category;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class CategoryRepositoryAdapter implements CategoryRepository {

    private final CategoryJpaRepository jpa;

    public CategoryRepositoryAdapter(CategoryJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public List<Category> findAll() {
        return jpa.findAll().stream().map(CategoryEntity::toDomain).toList();
    }

    @Override
    public Optional<Category> findByName(String name) {
        return jpa.findByName(name).map(CategoryEntity::toDomain);
    }

    @Override
    public Category save(Category category) {
        return jpa.save(CategoryEntity.fromDomain(category)).toDomain();
    }
}

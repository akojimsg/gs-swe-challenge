package com.gsswec.ecommerce.products.infrastructure.persistence;

import com.gsswec.ecommerce.products.domain.model.Category;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "categories", schema = "products_schema")
public class CategoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true)
    private String name;

    protected CategoryEntity() {
    }

    static CategoryEntity fromDomain(Category c) {
        CategoryEntity e = new CategoryEntity();
        e.id = c.id();
        e.name = c.name();
        return e;
    }

    Category toDomain() {
        return new Category(id, name);
    }
}

package com.gsswec.ecommerce.products.infrastructure.persistence;

import com.gsswec.ecommerce.products.domain.model.Product;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "products", schema = "products_schema")
public class ProductEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String sku;

    @Column
    private String description;

    @Column(name = "category_id")
    private Integer categoryId;

    @Column(nullable = false)
    private BigDecimal price;

    @Column(nullable = false)
    private Integer stock;

    @Column(name = "weight_kg")
    private BigDecimal weightKg;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private Instant updatedAt;

    protected ProductEntity() {
    }

    static ProductEntity fromDomain(Product p) {
        ProductEntity e = new ProductEntity();
        e.id = p.id() == null ? UUID.randomUUID() : p.id();
        e.name = p.name();
        e.sku = p.sku();
        e.description = p.description();
        e.categoryId = p.categoryId();
        e.price = p.price();
        e.stock = p.stock();
        e.weightKg = p.weightKg();
        e.active = p.active();
        return e;
    }

    Product toDomain() {
        return new Product(id, name, sku, description, categoryId,
                price, stock, weightKg, active, createdAt, updatedAt);
    }
}

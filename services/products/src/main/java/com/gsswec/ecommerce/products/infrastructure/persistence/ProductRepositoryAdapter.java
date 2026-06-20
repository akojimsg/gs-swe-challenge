package com.gsswec.ecommerce.products.infrastructure.persistence;

import com.gsswec.ecommerce.products.application.port.out.Page;
import com.gsswec.ecommerce.products.application.port.out.ProductRepository;
import com.gsswec.ecommerce.products.application.port.out.ProductSearchCriteria;
import com.gsswec.ecommerce.products.domain.model.Product;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
public class ProductRepositoryAdapter implements ProductRepository {

    // Whitelist of sortable columns; anything else falls back to name to avoid
    // SQL injection via the sort parameter and invalid-column errors.
    private static final Set<String> SORTABLE = Set.of("name", "price", "created_at", "stock");

    private final ProductJpaRepository jpa;

    public ProductRepositoryAdapter(ProductJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Product save(Product product) {
        return jpa.save(ProductEntity.fromDomain(product)).toDomain();
    }

    @Override
    public Optional<Product> findById(UUID id) {
        return jpa.findById(id).map(ProductEntity::toDomain);
    }

    @Override
    public Optional<Product> findBySku(String sku) {
        return jpa.findBySku(sku).map(ProductEntity::toDomain);
    }

    @Override
    public boolean existsBySku(String sku) {
        return jpa.existsBySku(sku);
    }

    @Override
    public void deleteById(UUID id) {
        jpa.deleteById(id);
    }

    @Override
    public Page<Product> search(ProductSearchCriteria c) {
        var entityPage = jpa.search(
                blankToNull(c.query()), blankToNull(c.category()),
                c.minPrice(), c.maxPrice(), c.inStock(),
                PageRequest.of(c.page(), c.size(), sort(c.sort())));
        List<Product> content = entityPage.map(ProductEntity::toDomain).getContent();
        return new Page<>(content, c.page(), c.size(),
                entityPage.getTotalElements(), entityPage.getTotalPages());
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    private static Sort sort(String sortParam) {
        if (sortParam == null || sortParam.isBlank()) {
            return Sort.by("name").ascending();
        }
        String[] parts = sortParam.split(",", 2);
        String column = parts[0].trim();
        if (!SORTABLE.contains(column)) {
            return Sort.by("name").ascending();
        }
        boolean desc = parts.length > 1 && parts[1].trim().equalsIgnoreCase("desc");
        return desc ? Sort.by(column).descending() : Sort.by(column).ascending();
    }
}

package com.gsswec.ecommerce.products.infrastructure.persistence;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductJpaRepository extends JpaRepository<ProductEntity, UUID> {

    Optional<ProductEntity> findBySku(String sku);

    boolean existsBySku(String sku);

    // Dynamic catalogue search. Each filter is bypassed when its parameter is null.
    // Full-text search uses the GIN-indexed tsvector when :q is provided.
    // Native query so the Postgres FTS operators are available; only active
    // products are ever returned to the public catalogue.
    @Query(value = """
            SELECT p.* FROM products_schema.products p
            LEFT JOIN products_schema.categories c ON c.id = p.category_id
            WHERE p.active = true
              AND (:q IS NULL OR to_tsvector('english', p.name || ' ' || COALESCE(p.description, ''))
                   @@ plainto_tsquery('english', :q))
              AND (:category IS NULL OR c.name = :category)
              AND (CAST(:minPrice AS numeric) IS NULL OR p.price >= :minPrice)
              AND (CAST(:maxPrice AS numeric) IS NULL OR p.price <= :maxPrice)
              AND (:inStock IS NULL OR (:inStock = true AND p.stock > 0) OR (:inStock = false))
            """,
            countQuery = """
            SELECT count(*) FROM products_schema.products p
            LEFT JOIN products_schema.categories c ON c.id = p.category_id
            WHERE p.active = true
              AND (:q IS NULL OR to_tsvector('english', p.name || ' ' || COALESCE(p.description, ''))
                   @@ plainto_tsquery('english', :q))
              AND (:category IS NULL OR c.name = :category)
              AND (CAST(:minPrice AS numeric) IS NULL OR p.price >= :minPrice)
              AND (CAST(:maxPrice AS numeric) IS NULL OR p.price <= :maxPrice)
              AND (:inStock IS NULL OR (:inStock = true AND p.stock > 0) OR (:inStock = false))
            """,
            nativeQuery = true)
    Page<ProductEntity> search(
            @Param("q") String q,
            @Param("category") String category,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("inStock") Boolean inStock,
            Pageable pageable);
}

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

    // Atomic conditional decrement: succeeds (returns 1) only when sufficient
    // active stock exists; the WHERE row-locks for the update, so concurrent
    // reservations of the last unit serialise and exactly one wins. Returns 0
    // when stock is insufficient or the SKU is missing/inactive.
    @org.springframework.data.jpa.repository.Modifying
    @Query(value = """
            UPDATE products_schema.products
               SET stock = stock - :qty, updated_at = now()
             WHERE sku = :sku AND active = true AND stock >= :qty
            """, nativeQuery = true)
    int tryDecrement(@Param("sku") String sku, @Param("qty") int qty);

    // Compensating increment (saga release). Unconditional restore of stock.
    @org.springframework.data.jpa.repository.Modifying
    @Query(value = """
            UPDATE products_schema.products
               SET stock = stock + :qty, updated_at = now()
             WHERE sku = :sku
            """, nativeQuery = true)
    int increment(@Param("sku") String sku, @Param("qty") int qty);

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

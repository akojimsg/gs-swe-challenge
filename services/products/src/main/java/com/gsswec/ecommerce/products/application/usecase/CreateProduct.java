package com.gsswec.ecommerce.products.application.usecase;

import com.gsswec.ecommerce.products.application.port.out.EventPublisher;
import com.gsswec.ecommerce.products.application.port.out.ProductCache;
import com.gsswec.ecommerce.products.application.port.out.ProductRepository;
import com.gsswec.ecommerce.products.domain.exception.DuplicateSkuException;
import com.gsswec.ecommerce.products.domain.model.Product;
import com.gsswec.ecommerce.shared.constants.StreamNames;
import com.gsswec.ecommerce.shared.events.product.ProductCreatedEvent;
import java.math.BigDecimal;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreateProduct {

    private final ProductRepository products;
    private final ResolveCategory resolveCategory;
    private final ProductCache cache;
    private final EventPublisher events;
    private final Clock clock;

    public CreateProduct(
            ProductRepository products, ResolveCategory resolveCategory,
            ProductCache cache, EventPublisher events, Clock clock) {
        this.products = products;
        this.resolveCategory = resolveCategory;
        this.cache = cache;
        this.events = events;
        this.clock = clock;
    }

    @Transactional
    public Product create(Command c) {
        String sku = c.sku().trim();
        if (products.existsBySku(sku)) {
            throw new DuplicateSkuException(sku);
        }

        Integer categoryId = resolveCategory.toIdOrNull(c.category());
        Product saved = products.save(Product.create(
                c.name().trim(), sku, c.description(), categoryId,
                c.price(), c.stock(), c.weightKg()));

        cache.evictAll();
        events.publish(StreamNames.PRODUCT_CREATED, new ProductCreatedEvent(
                Events.base(StreamNames.PRODUCT_CREATED, clock),
                saved.id(), saved.name(), saved.sku(), saved.price(),
                saved.stock(), c.category()));

        return saved;
    }

    public record Command(
            String name, String sku, String description, String category,
            BigDecimal price, Integer stock, BigDecimal weightKg) {
    }
}

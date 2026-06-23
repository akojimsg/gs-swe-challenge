package com.gsswec.ecommerce.products.application.usecase;

import com.gsswec.ecommerce.products.application.port.out.EventPublisher;
import com.gsswec.ecommerce.products.application.port.out.ProductCache;
import com.gsswec.ecommerce.products.application.port.out.ProductRepository;
import com.gsswec.ecommerce.products.domain.exception.ProductNotFoundException;
import com.gsswec.ecommerce.products.domain.model.Product;
import com.gsswec.ecommerce.shared.constants.StreamNames;
import com.gsswec.ecommerce.shared.events.product.ProductUpdatedEvent;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UpdateProduct {

    private final ProductRepository products;
    private final ResolveCategory resolveCategory;
    private final ProductCache cache;
    private final EventPublisher events;
    private final Clock clock;

    public UpdateProduct(
            ProductRepository products, ResolveCategory resolveCategory,
            ProductCache cache, EventPublisher events, Clock clock) {
        this.products = products;
        this.resolveCategory = resolveCategory;
        this.cache = cache;
        this.events = events;
        this.clock = clock;
    }

    // PATCH semantics: a null field means "leave unchanged".
    // PUT semantics: callers pass every field (the controller supplies them), so a
    // null there genuinely clears the value. Both route through here with `partial`.
    @Transactional
    public Product update(UUID id, Command c, boolean partial) {
        Product current = products.findById(id).orElseThrow(ProductNotFoundException::new);

        String name = resolve(c.name(), current.name(), partial);
        String description = resolve(c.description(), current.description(), partial);
        BigDecimal price = resolve(c.price(), current.price(), partial);
        Integer stock = resolve(c.stock(), current.stock(), partial);
        BigDecimal weightKg = resolve(c.weightKg(), current.weightKg(), partial);
        String imageUrl = resolve(c.imageUrl(), current.imageUrl(), partial);
        Boolean active = resolve(c.active(), current.active(), partial);

        Integer categoryId = current.categoryId();
        boolean categoryProvided = c.category() != null || !partial;
        if (categoryProvided) {
            categoryId = resolveCategory.toIdOrNull(c.category());
        }

        Product updated = products.save(current.withUpdates(
                name != null ? name.trim() : current.name(),
                description, categoryId, price, stock, weightKg, imageUrl, active));

        cache.evictAll();

        Map<String, Object> changed = diff(current, updated, categoryProvided ? c.category() : null);
        if (!changed.isEmpty()) {
            events.publish(StreamNames.PRODUCT_UPDATED, new ProductUpdatedEvent(
                    Events.base(StreamNames.PRODUCT_UPDATED, clock), updated.id(), changed));
        }
        return updated;
    }

    private static <T> T resolve(T incoming, T currentValue, boolean partial) {
        return (incoming == null && partial) ? currentValue : incoming;
    }

    private static Map<String, Object> diff(Product before, Product after, String categoryName) {
        Map<String, Object> changed = new HashMap<>();
        if (!Objects.equals(before.name(), after.name())) {
            changed.put("name", after.name());
        }
        if (!Objects.equals(before.description(), after.description())) {
            changed.put("description", after.description());
        }
        if (before.price().compareTo(after.price()) != 0) {
            changed.put("price", after.price());
        }
        if (!Objects.equals(before.stock(), after.stock())) {
            changed.put("stock", after.stock());
        }
        if (!Objects.equals(before.categoryId(), after.categoryId())) {
            changed.put("category", categoryName);
        }
        if (before.active() != after.active()) {
            changed.put("active", after.active());
        }
        return changed;
    }

    public record Command(
            String name, String description, String category,
            BigDecimal price, Integer stock, BigDecimal weightKg, String imageUrl, Boolean active) {
    }
}

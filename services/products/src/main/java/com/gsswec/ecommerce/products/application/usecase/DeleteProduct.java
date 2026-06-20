package com.gsswec.ecommerce.products.application.usecase;

import com.gsswec.ecommerce.products.application.port.out.EventPublisher;
import com.gsswec.ecommerce.products.application.port.out.ProductCache;
import com.gsswec.ecommerce.products.application.port.out.ProductRepository;
import com.gsswec.ecommerce.products.domain.exception.ProductNotFoundException;
import com.gsswec.ecommerce.products.domain.model.Product;
import com.gsswec.ecommerce.shared.constants.StreamNames;
import com.gsswec.ecommerce.shared.events.product.ProductDeletedEvent;
import java.time.Clock;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeleteProduct {

    private final ProductRepository products;
    private final ProductCache cache;
    private final EventPublisher events;
    private final Clock clock;

    public DeleteProduct(
            ProductRepository products, ProductCache cache, EventPublisher events, Clock clock) {
        this.products = products;
        this.cache = cache;
        this.events = events;
        this.clock = clock;
    }

    @Transactional
    public void delete(UUID id) {
        Product current = products.findById(id).orElseThrow(ProductNotFoundException::new);
        products.deleteById(id);

        cache.evictAll();
        events.publish(StreamNames.PRODUCT_DELETED, new ProductDeletedEvent(
                Events.base(StreamNames.PRODUCT_DELETED, clock), current.id(), current.sku()));
    }
}

package com.gsswec.ecommerce.products.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gsswec.ecommerce.products.application.port.out.EventPublisher;
import com.gsswec.ecommerce.products.application.port.out.ProductCache;
import com.gsswec.ecommerce.products.application.port.out.ProductRepository;
import com.gsswec.ecommerce.products.application.usecase.ResolveCategory;
import com.gsswec.ecommerce.products.application.usecase.UpdateProduct;
import com.gsswec.ecommerce.products.domain.model.Product;
import com.gsswec.ecommerce.shared.constants.StreamNames;
import com.gsswec.ecommerce.shared.events.base.DomainEvent;
import com.gsswec.ecommerce.shared.events.product.ProductUpdatedEvent;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class UpdateProductTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-06-20T00:00:00Z"), ZoneOffset.UTC);
    private final UUID id = UUID.randomUUID();
    private ProductRepository products;
    private ResolveCategory resolveCategory;
    private ProductCache cache;
    private EventPublisher events;
    private UpdateProduct updateProduct;

    @BeforeEach
    void setUp() {
        products = org.mockito.Mockito.mock(ProductRepository.class);
        resolveCategory = org.mockito.Mockito.mock(ResolveCategory.class);
        cache = org.mockito.Mockito.mock(ProductCache.class);
        events = org.mockito.Mockito.mock(EventPublisher.class);
        updateProduct = new UpdateProduct(products, resolveCategory, cache, events, clock);
        when(products.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private Product existing() {
        return new Product(id, "Old Name", "SKU-1", "old desc", 1,
                new BigDecimal("10.00"), 5, new BigDecimal("0.5"), null, true,
                Instant.now(clock), Instant.now(clock));
    }

    @Test
    void patchOnlyChangesProvidedFieldsAndPublishesChangedSet() {
        when(products.findById(id)).thenReturn(Optional.of(existing()));

        // PATCH: only price provided.
        var cmd = new UpdateProduct.Command(null, null, null, new BigDecimal("12.50"), null, null, null, null);
        Product result = updateProduct.update(id, cmd, true);

        assertThat(result.price()).isEqualByComparingTo("12.50");
        assertThat(result.name()).isEqualTo("Old Name");          // unchanged
        assertThat(result.stock()).isEqualTo(5);                   // unchanged

        ArgumentCaptor<DomainEvent> ev = ArgumentCaptor.forClass(DomainEvent.class);
        verify(events).publish(org.mockito.ArgumentMatchers.eq(StreamNames.PRODUCT_UPDATED), ev.capture());
        var changed = ((ProductUpdatedEvent) ev.getValue()).changedFields();
        assertThat(changed).containsOnlyKeys("price");
        verify(cache).evictAll();
    }

    @Test
    void patchWithNoEffectiveChangePublishesNoEvent() {
        when(products.findById(id)).thenReturn(Optional.of(existing()));

        // PATCH price to its current value → no change.
        var cmd = new UpdateProduct.Command(null, null, null, new BigDecimal("10.00"), null, null, null, null);
        updateProduct.update(id, cmd, true);

        verify(events, never()).publish(anyString(), any());
    }
}

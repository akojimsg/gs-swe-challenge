package com.gsswec.ecommerce.shared.events;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.gsswec.ecommerce.shared.dto.OrderItemSnapshot;
import com.gsswec.ecommerce.shared.events.base.BaseEvent;
import com.gsswec.ecommerce.shared.events.order.OrderPlacedEvent;
import com.gsswec.ecommerce.shared.events.product.ProductImportedEvent;
import com.gsswec.ecommerce.shared.events.product.ProductUpdatedEvent;
import com.gsswec.ecommerce.shared.util.ImportError;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EventImmutabilityTest {

    private static BaseEvent base() {
        return new BaseEvent(UUID.randomUUID(), "test", "1.0", Instant.EPOCH, "trace", "test-svc");
    }

    private static OrderItemSnapshot item() {
        return new OrderItemSnapshot(UUID.randomUUID(), "SKU-1", "Widget", 2, new BigDecimal("9.99"));
    }

    @Test
    void orderPlacedCopiesItemListDefensively() {
        List<OrderItemSnapshot> source = new ArrayList<>(List.of(item()));
        OrderPlacedEvent event = new OrderPlacedEvent(base(), UUID.randomUUID(), UUID.randomUUID(),
                source, new BigDecimal("19.98"));

        source.add(item());

        assertThat(event.items()).hasSize(1);
    }

    @Test
    void orderPlacedItemsAreUnmodifiable() {
        OrderPlacedEvent event = new OrderPlacedEvent(base(), UUID.randomUUID(), UUID.randomUUID(),
                List.of(item()), new BigDecimal("9.99"));

        assertThatThrownBy(() -> event.items().add(item()))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void nullCollectionsBecomeEmpty() {
        OrderPlacedEvent order = new OrderPlacedEvent(base(), UUID.randomUUID(), UUID.randomUUID(),
                null, BigDecimal.ZERO);
        ProductImportedEvent imported = new ProductImportedEvent(base(), UUID.randomUUID(),
                UUID.randomUUID(), 0, 0, 0, 0, null, 0L);
        ProductUpdatedEvent updated = new ProductUpdatedEvent(base(), UUID.randomUUID(), null);

        assertThat(order.items()).isEmpty();
        assertThat(imported.errors()).isEmpty();
        assertThat(updated.changedFields()).isEmpty();
    }

    @Test
    void productImportedCopiesErrorListDefensively() {
        List<ImportError> source = new ArrayList<>(List.of(new ImportError(1, "sku", "", "missing")));
        ProductImportedEvent event = new ProductImportedEvent(base(), UUID.randomUUID(),
                UUID.randomUUID(), 1, 0, 0, 1, source, 5L);

        source.clear();

        assertThat(event.errors()).hasSize(1);
        assertThatThrownBy(() -> event.errors().add(new ImportError(2, "x", "", "y")))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void productUpdatedCopiesChangedFieldsDefensively() {
        Map<String, Object> source = new HashMap<>(Map.of("price", new BigDecimal("1.00")));
        ProductUpdatedEvent event = new ProductUpdatedEvent(base(), UUID.randomUUID(), source);

        source.put("stock", 5);

        assertThat(event.changedFields()).containsOnlyKeys("price");
        assertThatThrownBy(() -> event.changedFields().put("x", 1))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void baseAccessorExposesEnvelope() {
        BaseEvent b = base();
        OrderPlacedEvent event = new OrderPlacedEvent(b, UUID.randomUUID(), UUID.randomUUID(),
                List.of(), BigDecimal.ZERO);

        assertThat(event.base()).isEqualTo(b);
    }
}

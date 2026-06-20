package com.gsswec.ecommerce.shared.events.product;

import com.gsswec.ecommerce.shared.events.base.BaseEvent;
import com.gsswec.ecommerce.shared.events.base.DomainEvent;
import com.gsswec.ecommerce.shared.util.ImportError;
import java.util.List;
import java.util.UUID;

public record ProductImportedEvent(
        BaseEvent base,
        UUID importId,
        UUID requestedBy,
        Integer totalRows,
        Integer imported,
        Integer updated,
        Integer skipped,
        List<ImportError> errors,
        Long durationMs) implements DomainEvent {

    public ProductImportedEvent {
        errors = errors == null ? List.of() : List.copyOf(errors);
    }
}

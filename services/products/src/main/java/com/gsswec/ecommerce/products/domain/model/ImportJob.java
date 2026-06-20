package com.gsswec.ecommerce.products.domain.model;

import com.gsswec.ecommerce.shared.util.ImportError;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ImportJob(
        UUID id,
        UUID requestedBy,
        Status status,
        Integer totalRows,
        Integer imported,
        Integer updated,
        Integer skipped,
        List<ImportError> errors,
        Long durationMs,
        Instant createdAt,
        Instant completedAt) {

    public enum Status {
        PROCESSING, COMPLETED, FAILED
    }

    public ImportJob {
        errors = errors == null ? List.of() : List.copyOf(errors);
    }

    public static ImportJob processing(UUID id, UUID requestedBy) {
        return new ImportJob(id, requestedBy, Status.PROCESSING,
                null, null, null, null, List.of(), null, null, null);
    }

    public ImportJob completed(int totalRows, int imported, int updated, int skipped,
            List<ImportError> errors, long durationMs) {
        return new ImportJob(id, requestedBy, Status.COMPLETED,
                totalRows, imported, updated, skipped, errors, durationMs, createdAt, Instant.now());
    }

    public ImportJob failed(long durationMs) {
        return new ImportJob(id, requestedBy, Status.FAILED,
                totalRows, imported, updated, skipped, errors, durationMs, createdAt, Instant.now());
    }
}

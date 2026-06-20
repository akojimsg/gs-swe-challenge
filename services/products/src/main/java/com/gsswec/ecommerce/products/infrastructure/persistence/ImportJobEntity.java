package com.gsswec.ecommerce.products.infrastructure.persistence;

import com.gsswec.ecommerce.products.domain.model.ImportJob;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "import_jobs", schema = "products_schema")
public class ImportJobEntity {

    @Id
    private UUID id;

    @Column(name = "requested_by", nullable = false)
    private UUID requestedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ImportJob.Status status;

    @Column(name = "total_rows")
    private Integer totalRows;

    private Integer imported;
    private Integer updated;
    private Integer skipped;

    // JSON-serialised List<ImportError>; mapped to/from domain in the adapter.
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String errors;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    protected ImportJobEntity() {
    }

    static ImportJobEntity create(UUID id, UUID requestedBy, ImportJob.Status status, String errorsJson) {
        ImportJobEntity e = new ImportJobEntity();
        e.id = id;
        e.requestedBy = requestedBy;
        e.status = status;
        e.errors = errorsJson;
        return e;
    }

    void apply(ImportJob job, String errorsJson) {
        this.status = job.status();
        this.totalRows = job.totalRows();
        this.imported = job.imported();
        this.updated = job.updated();
        this.skipped = job.skipped();
        this.errors = errorsJson;
        this.durationMs = job.durationMs();
        this.completedAt = job.completedAt();
    }

    UUID id() {
        return id;
    }

    UUID requestedBy() {
        return requestedBy;
    }

    ImportJob.Status status() {
        return status;
    }

    Integer totalRows() {
        return totalRows;
    }

    Integer imported() {
        return imported;
    }

    Integer updated() {
        return updated;
    }

    Integer skipped() {
        return skipped;
    }

    String errorsJson() {
        return errors;
    }

    Long durationMs() {
        return durationMs;
    }

    Instant createdAt() {
        return createdAt;
    }

    Instant completedAt() {
        return completedAt;
    }
}

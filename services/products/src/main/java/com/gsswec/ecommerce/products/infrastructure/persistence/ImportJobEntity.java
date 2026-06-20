package com.gsswec.ecommerce.products.infrastructure.persistence;

import com.gsswec.ecommerce.products.domain.model.ImportJob;
import com.gsswec.ecommerce.shared.util.ImportError;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.List;
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

    // Mirror of ProductEntity/UserEntity: entity<->domain mapping lives on the
    // entity, not the adapter. The errors column is the one seam — it is JSON, so
    // the adapter passes the serialised string in and reads it back out (it owns
    // Jackson); everything else maps here.
    static ImportJobEntity fromDomain(ImportJob job, String errorsJson) {
        ImportJobEntity e = new ImportJobEntity();
        e.id = job.id();
        e.requestedBy = job.requestedBy();
        e.status = job.status();
        e.totalRows = job.totalRows();
        e.imported = job.imported();
        e.updated = job.updated();
        e.skipped = job.skipped();
        e.errors = errorsJson;
        e.durationMs = job.durationMs();
        e.completedAt = job.completedAt();
        return e;
    }

    ImportJob toDomain(List<ImportError> errors) {
        return new ImportJob(id, requestedBy, status, totalRows, imported, updated,
                skipped, errors, durationMs, createdAt, completedAt);
    }

    // The only accessor: the raw JSON the adapter deserialises into ImportErrors.
    String errorsJson() {
        return errors;
    }
}

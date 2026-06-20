package com.gsswec.ecommerce.products.infrastructure.persistence;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gsswec.ecommerce.products.application.port.out.ImportJobRepository;
import com.gsswec.ecommerce.products.domain.model.ImportJob;
import com.gsswec.ecommerce.shared.util.ImportError;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class ImportJobRepositoryAdapter implements ImportJobRepository {

    private final ImportJobJpaRepository jpa;
    private final ObjectMapper objectMapper;

    public ImportJobRepositoryAdapter(ImportJobJpaRepository jpa, ObjectMapper objectMapper) {
        this.jpa = jpa;
        this.objectMapper = objectMapper;
    }

    @Override
    public ImportJob save(ImportJob job) {
        String errorsJson = writeErrors(job.errors());
        ImportJobEntity entity = jpa.findById(job.id())
                .orElseGet(() -> ImportJobEntity.create(job.id(), job.requestedBy(), job.status(), errorsJson));
        entity.apply(job, errorsJson);
        return toDomain(jpa.save(entity));
    }

    @Override
    public Optional<ImportJob> findById(UUID id) {
        return jpa.findById(id).map(this::toDomain);
    }

    private ImportJob toDomain(ImportJobEntity e) {
        return new ImportJob(e.id(), e.requestedBy(), e.status(),
                e.totalRows(), e.imported(), e.updated(), e.skipped(),
                readErrors(e.errorsJson()), e.durationMs(), e.createdAt(), e.completedAt());
    }

    private String writeErrors(List<ImportError> errors) {
        try {
            return objectMapper.writeValueAsString(errors == null ? List.of() : errors);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialise import errors", e);
        }
    }

    private List<ImportError> readErrors(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<ImportError>>() { });
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            return List.of();
        }
    }
}

package com.gsswec.ecommerce.products.api.rest.dto;

import com.gsswec.ecommerce.products.domain.model.ImportJob;
import com.gsswec.ecommerce.shared.util.ImportError;
import java.util.List;
import java.util.UUID;

public record ImportJobResponse(
        UUID importId,
        String status,
        Integer totalRows,
        Integer imported,
        Integer updated,
        Integer skipped,
        List<ImportError> errors,
        Long durationMs) {

    public static ImportJobResponse from(ImportJob j) {
        return new ImportJobResponse(j.id(), j.status().name(),
                j.totalRows(), j.imported(), j.updated(), j.skipped(), j.errors(), j.durationMs());
    }
}

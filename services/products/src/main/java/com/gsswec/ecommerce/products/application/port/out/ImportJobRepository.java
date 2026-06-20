package com.gsswec.ecommerce.products.application.port.out;

import com.gsswec.ecommerce.products.domain.model.ImportJob;
import java.util.Optional;
import java.util.UUID;

public interface ImportJobRepository {

    ImportJob save(ImportJob job);

    Optional<ImportJob> findById(UUID id);
}

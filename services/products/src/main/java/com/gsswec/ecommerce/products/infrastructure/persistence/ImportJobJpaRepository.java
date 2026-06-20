package com.gsswec.ecommerce.products.infrastructure.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImportJobJpaRepository extends JpaRepository<ImportJobEntity, UUID> {
}

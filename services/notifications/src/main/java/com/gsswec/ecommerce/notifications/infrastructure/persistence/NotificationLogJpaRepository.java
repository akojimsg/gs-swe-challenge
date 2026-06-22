package com.gsswec.ecommerce.notifications.infrastructure.persistence;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationLogJpaRepository extends JpaRepository<NotificationLogEntity, UUID> {
    boolean existsByEventId(UUID eventId);
    Page<NotificationLogEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);
}

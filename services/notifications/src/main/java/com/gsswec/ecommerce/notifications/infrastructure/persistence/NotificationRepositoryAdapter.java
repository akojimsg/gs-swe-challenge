package com.gsswec.ecommerce.notifications.infrastructure.persistence;

import com.gsswec.ecommerce.notifications.application.port.out.NotificationRepository;
import com.gsswec.ecommerce.notifications.domain.model.NotificationLog;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@Component
public class NotificationRepositoryAdapter implements NotificationRepository {

    private final NotificationLogJpaRepository jpa;

    public NotificationRepositoryAdapter(NotificationLogJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public void save(NotificationLog log) {
        jpa.save(new NotificationLogEntity(
                log.eventId(), log.eventType(), log.recipient(),
                log.template(), log.status(), log.error(), log.createdAt()));
    }

    @Override
    public boolean existsByEventId(UUID eventId) {
        return jpa.existsByEventId(eventId);
    }

    @Override
    public List<NotificationLog> findAll(int page, int size) {
        return jpa.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size))
                .map(NotificationLogEntity::toDomain)
                .toList();
    }

    @Override
    public long count() {
        return jpa.count();
    }
}

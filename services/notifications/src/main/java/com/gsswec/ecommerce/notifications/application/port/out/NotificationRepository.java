package com.gsswec.ecommerce.notifications.application.port.out;

import com.gsswec.ecommerce.notifications.domain.model.NotificationLog;
import java.util.List;
import java.util.UUID;

public interface NotificationRepository {
    void save(NotificationLog log);
    boolean existsByEventId(UUID eventId);
    List<NotificationLog> findAll(int page, int size);
    long count();
}

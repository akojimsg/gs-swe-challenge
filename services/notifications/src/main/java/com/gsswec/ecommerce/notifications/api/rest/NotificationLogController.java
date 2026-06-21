package com.gsswec.ecommerce.notifications.api.rest;

import com.gsswec.ecommerce.notifications.api.rest.dto.NotificationLogResponse;
import com.gsswec.ecommerce.notifications.application.port.out.NotificationRepository;
import com.gsswec.ecommerce.notifications.domain.model.NotificationLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
@Tag(name = "Notifications", description = "Notification delivery log — ADMIN only")
public class NotificationLogController {

    private final NotificationRepository repository;

    public NotificationLogController(NotificationRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/logs")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "List notification delivery logs",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<List<NotificationLogResponse>> getLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        List<NotificationLogResponse> logs = repository.findAll(page, size).stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(logs);
    }

    private NotificationLogResponse toResponse(NotificationLog log) {
        return new NotificationLogResponse(
                log.id(), log.eventId(), log.eventType(), log.recipient(),
                log.template(), log.status(), log.error(), log.createdAt());
    }
}

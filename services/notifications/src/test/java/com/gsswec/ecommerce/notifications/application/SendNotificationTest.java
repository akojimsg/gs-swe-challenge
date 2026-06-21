package com.gsswec.ecommerce.notifications.application;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gsswec.ecommerce.notifications.application.port.out.EmailSender;
import com.gsswec.ecommerce.notifications.application.port.out.NotificationRepository;
import com.gsswec.ecommerce.notifications.application.usecase.SendNotification;
import com.gsswec.ecommerce.notifications.domain.model.EmailTemplate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SendNotificationTest {

    @Mock NotificationRepository repository;
    @Mock EmailSender emailSender;
    @InjectMocks SendNotification useCase;

    @Test
    void sendsEmailAndPersistsLog() {
        UUID eventId = UUID.randomUUID();
        when(repository.existsByEventId(eventId)).thenReturn(false);

        useCase.execute(eventId, "user.registered", "user@example.com",
                EmailTemplate.WELCOME, "Welcome!");

        verify(emailSender).send("user@example.com", EmailTemplate.WELCOME, "Welcome!");
        verify(repository).save(any());
    }

    @Test
    void skipsAlreadyProcessedEvent() {
        UUID eventId = UUID.randomUUID();
        when(repository.existsByEventId(eventId)).thenReturn(true);

        useCase.execute(eventId, "user.registered", "user@example.com",
                EmailTemplate.WELCOME, "Welcome!");

        verify(emailSender, never()).send(any(), any(), any());
        verify(repository, never()).save(any());
    }

    @Test
    void persistsFailedStatusWhenMailThrows() {
        UUID eventId = UUID.randomUUID();
        when(repository.existsByEventId(eventId)).thenReturn(false);
        doThrow(new RuntimeException("SMTP down"))
                .when(emailSender).send(any(), any(), any());

        useCase.execute(eventId, "order.paid", "buyer@example.com",
                EmailTemplate.ORDER_PAID, "Your order is confirmed");

        verify(repository).save(argThat(log ->
                "FAILED".equals(log.status()) && "SMTP down".equals(log.error())));
    }

    private static <T> T argThat(java.util.function.Predicate<T> predicate) {
        return org.mockito.ArgumentMatchers.argThat(predicate::test);
    }
}

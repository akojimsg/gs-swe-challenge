package com.gsswec.ecommerce.notifications.application.port.out;

import com.gsswec.ecommerce.notifications.domain.model.EmailTemplate;

public interface EmailSender {
    void send(String to, EmailTemplate template, String body);
}

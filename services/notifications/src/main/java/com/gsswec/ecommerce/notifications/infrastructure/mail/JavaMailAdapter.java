package com.gsswec.ecommerce.notifications.infrastructure.mail;

import com.gsswec.ecommerce.notifications.application.port.out.EmailSender;
import com.gsswec.ecommerce.notifications.domain.model.EmailTemplate;
import com.gsswec.ecommerce.notifications.infrastructure.config.NotificationsProperties;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
public class JavaMailAdapter implements EmailSender {

    private final JavaMailSender mailSender;
    private final NotificationsProperties properties;

    public JavaMailAdapter(JavaMailSender mailSender, NotificationsProperties properties) {
        this.mailSender = mailSender;
        this.properties = properties;
    }

    @Override
    public void send(String to, EmailTemplate template, String body) {
        var message = new SimpleMailMessage();
        message.setFrom(properties.fromAddress());
        message.setTo(to);
        message.setSubject(template.subject);
        message.setText(body);
        mailSender.send(message);
    }
}

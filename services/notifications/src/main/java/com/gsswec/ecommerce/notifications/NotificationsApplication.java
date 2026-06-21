package com.gsswec.ecommerce.notifications;

import com.gsswec.ecommerce.notifications.infrastructure.config.JwtProperties;
import com.gsswec.ecommerce.notifications.infrastructure.config.NotificationsProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({JwtProperties.class, NotificationsProperties.class})
public class NotificationsApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationsApplication.class, args);
    }
}

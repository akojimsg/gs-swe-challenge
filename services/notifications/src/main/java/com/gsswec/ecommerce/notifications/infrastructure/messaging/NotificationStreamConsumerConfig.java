package com.gsswec.ecommerce.notifications.infrastructure.messaging;

import com.gsswec.ecommerce.notifications.api.event.NotificationEventListener;
import com.gsswec.ecommerce.shared.constants.StreamNames;
import java.time.Duration;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.data.redis.stream.StreamMessageListenerContainer.StreamMessageListenerContainerOptions;

@Configuration
public class NotificationStreamConsumerConfig {

    private static final Logger log = LoggerFactory.getLogger(NotificationStreamConsumerConfig.class);

    static final String GROUP = "notifications";
    static final String CONSUMER = "notifications-1";

    private static final List<String> SUBSCRIBED_STREAMS = List.of(
            StreamNames.USER_REGISTERED,
            StreamNames.USER_ROLE_CHANGED,
            StreamNames.PRODUCT_CREATED,
            StreamNames.PRODUCT_STOCK_LOW,
            StreamNames.PRODUCT_IMPORTED,
            StreamNames.ORDER_PAID,
            StreamNames.ORDER_FAILED,
            StreamNames.PAYMENT_SUCCEEDED,
            StreamNames.PAYMENT_FAILED);

    @Bean
    StreamMessageListenerContainer<String, MapRecord<String, String, String>> notificationListenerContainer(
            RedisConnectionFactory connectionFactory,
            StringRedisTemplate redis,
            NotificationEventListener listener) {

        SUBSCRIBED_STREAMS.forEach(stream -> ensureGroup(redis, stream));

        var options = StreamMessageListenerContainerOptions.builder()
                .pollTimeout(Duration.ofSeconds(1))
                .build();

        StreamMessageListenerContainer<String, MapRecord<String, String, String>> container =
                StreamMessageListenerContainer.create(connectionFactory, options);

        SUBSCRIBED_STREAMS.forEach(stream ->
                container.receiveAutoAck(
                        Consumer.from(GROUP, CONSUMER),
                        StreamOffset.create(stream, ReadOffset.lastConsumed()),
                        listener));

        container.start();
        return container;
    }

    private void ensureGroup(StringRedisTemplate redis, String stream) {
        try {
            redis.opsForStream().createGroup(stream, ReadOffset.from("0"), GROUP);
        } catch (Exception e) {
            log.debug("Consumer group {} on {} already exists: {}", GROUP, stream, e.getMessage());
        }
    }
}

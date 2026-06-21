package com.gsswec.ecommerce.orders.infrastructure.messaging;

import com.gsswec.ecommerce.shared.constants.StreamNames;
import java.time.Duration;
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

// Wires the Orders-side saga consumers. One consumer group ("orders") subscribes to
// both payment.succeeded and payment.failed for competing-consumer semantics.
// receiveAutoAck acks a record once onMessage returns normally, so it leaves the
// Pending Entries List; a thrown exception leaves it pending for redelivery.
@Configuration
public class StreamConsumerConfig {

    private static final Logger log = LoggerFactory.getLogger(StreamConsumerConfig.class);

    static final String GROUP = "orders";
    static final String CONSUMER = "orders-1";

    @Bean
    StreamMessageListenerContainer<String, MapRecord<String, String, String>> streamListenerContainer(
            RedisConnectionFactory connectionFactory,
            StringRedisTemplate redis,
            PaymentOutcomeConsumer paymentOutcomeConsumer) {

        ensureGroup(redis, StreamNames.PAYMENT_SUCCEEDED);
        ensureGroup(redis, StreamNames.PAYMENT_FAILED);

        var options = StreamMessageListenerContainerOptions.builder()
                .pollTimeout(Duration.ofSeconds(1))
                .build();

        StreamMessageListenerContainer<String, MapRecord<String, String, String>> container =
                StreamMessageListenerContainer.create(connectionFactory, options);

        container.receiveAutoAck(
                Consumer.from(GROUP, CONSUMER),
                StreamOffset.create(StreamNames.PAYMENT_SUCCEEDED, ReadOffset.lastConsumed()),
                paymentOutcomeConsumer);
        container.receiveAutoAck(
                Consumer.from(GROUP, CONSUMER),
                StreamOffset.create(StreamNames.PAYMENT_FAILED, ReadOffset.lastConsumed()),
                paymentOutcomeConsumer);

        container.start();
        return container;
    }

    // MKSTREAM-style create is idempotent across restarts; BUSYGROUP simply means the
    // group already exists, which is the steady state.
    private void ensureGroup(StringRedisTemplate redis, String stream) {
        try {
            redis.opsForStream().createGroup(stream, ReadOffset.from("0"), GROUP);
        } catch (Exception e) {
            log.debug("Consumer group {} on {} already exists (or stream pending): {}",
                    GROUP, stream, e.getMessage());
        }
    }
}

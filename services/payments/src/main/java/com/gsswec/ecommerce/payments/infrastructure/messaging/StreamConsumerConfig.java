package com.gsswec.ecommerce.payments.infrastructure.messaging;

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

// Wires a Redis Streams consumer group for the payments saga. One consumer group
// ("payments") on the order.placed stream gives us competing-consumer semantics and
// pending-entry tracking (at-least-once). The container auto-acks on normal return.
@Configuration
public class StreamConsumerConfig {

    private static final Logger log = LoggerFactory.getLogger(StreamConsumerConfig.class);

    static final String GROUP = "payments";
    static final String CONSUMER = "payments-1";

    @Bean
    StreamMessageListenerContainer<String, MapRecord<String, String, String>> streamListenerContainer(
            RedisConnectionFactory connectionFactory,
            StringRedisTemplate redis,
            OrderPlacedConsumer orderPlacedConsumer) {

        ensureGroup(redis, StreamNames.ORDER_PLACED);

        var options = StreamMessageListenerContainerOptions.builder()
                .pollTimeout(Duration.ofSeconds(1))
                .build();

        StreamMessageListenerContainer<String, MapRecord<String, String, String>> container =
                StreamMessageListenerContainer.create(connectionFactory, options);

        container.receive(
                Consumer.from(GROUP, CONSUMER),
                StreamOffset.create(StreamNames.ORDER_PLACED, ReadOffset.lastConsumed()),
                orderPlacedConsumer);

        container.start();
        return container;
    }

    // Creating the group with MKSTREAM is idempotent across restarts; the BUSYGROUP
    // error simply means the group already exists, which is the steady state.
    private void ensureGroup(StringRedisTemplate redis, String stream) {
        try {
            redis.opsForStream().createGroup(stream, ReadOffset.from("0"), GROUP);
        } catch (Exception e) {
            log.debug("Consumer group {} on {} already exists (or stream pending): {}",
                    GROUP, stream, e.getMessage());
        }
    }
}

package com.gsswec.ecommerce.products.infrastructure.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.gsswec.ecommerce.products.application.port.out.Page;
import com.gsswec.ecommerce.products.application.port.out.ProductCache;
import com.gsswec.ecommerce.products.application.port.out.ProductSearchCriteria;
import com.gsswec.ecommerce.products.domain.model.Product;
import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisProductCache implements ProductCache {

    private static final String KEY_PREFIX = "products:search:";
    private static final Duration TTL = Duration.ofSeconds(60);

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public RedisProductCache(StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<Page<Product>> getSearch(ProductSearchCriteria criteria) {
        String json = redis.opsForValue().get(key(criteria));
        if (json == null) {
            return Optional.empty();
        }
        try {
            TypeFactory tf = objectMapper.getTypeFactory();
            return Optional.of(objectMapper.readValue(json,
                    tf.constructParametricType(Page.class, Product.class)));
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            // A corrupt/incompatible cache entry should never break reads.
            return Optional.empty();
        }
    }

    @Override
    public void putSearch(ProductSearchCriteria criteria, Page<Product> result) {
        try {
            redis.opsForValue().set(key(criteria), objectMapper.writeValueAsString(result), TTL);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            // Caching is best-effort; a serialization failure must not fail the read.
        }
    }

    @Override
    public void evictAll() {
        Set<String> keys = redis.keys(KEY_PREFIX + "*");
        if (keys != null && !keys.isEmpty()) {
            redis.delete(keys);
        }
    }

    private String key(ProductSearchCriteria c) {
        return KEY_PREFIX + String.join("|",
                ns(c.query()), ns(c.category()), ns(c.minPrice()), ns(c.maxPrice()),
                ns(c.inStock()), String.valueOf(c.page()), String.valueOf(c.size()), ns(c.sort()));
    }

    private static String ns(Object o) {
        return o == null ? "" : o.toString();
    }
}

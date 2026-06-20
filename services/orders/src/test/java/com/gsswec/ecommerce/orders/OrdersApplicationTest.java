package com.gsswec.ecommerce.orders;

import org.junit.jupiter.api.Test;

class OrdersApplicationTest {

    @Test
    void mainClassIsLoadable() {
        // Scaffold-level guard: the bootstrap class is on the classpath.
        // Full @SpringBootTest context-load arrives with the feature issues
        // that add web/jpa/redis wiring.
        org.junit.jupiter.api.Assertions.assertNotNull(OrdersApplication.class);
    }
}

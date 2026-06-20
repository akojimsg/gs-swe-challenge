package com.gsswec.ecommerce.users;

import org.junit.jupiter.api.Test;

class UsersApplicationTest {

    @Test
    void mainClassIsLoadable() {
        // Scaffold-level guard: the bootstrap class is on the classpath.
        // Full @SpringBootTest context-load arrives with the feature issues
        // that add web/jpa/redis wiring.
        org.junit.jupiter.api.Assertions.assertNotNull(UsersApplication.class);
    }
}

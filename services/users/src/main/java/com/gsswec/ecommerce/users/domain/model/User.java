package com.gsswec.ecommerce.users.domain.model;

import java.time.Instant;
import java.util.UUID;

public record User(
        UUID id,
        String email,
        String passwordHash,
        String firstName,
        String lastName,
        Role role,
        boolean active,
        Instant createdAt,
        Instant updatedAt) {

    public static User register(
            String email, String passwordHash, String firstName, String lastName) {
        return new User(
                null, email, passwordHash, firstName, lastName,
                Role.BUYER, true, null, null);
    }

    public User withRole(Role newRole) {
        return new User(
                id, email, passwordHash, firstName, lastName,
                newRole, active, createdAt, updatedAt);
    }

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }
}

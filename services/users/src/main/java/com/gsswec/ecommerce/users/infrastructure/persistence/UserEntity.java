package com.gsswec.ecommerce.users.infrastructure.persistence;

import com.gsswec.ecommerce.users.domain.model.Role;
import com.gsswec.ecommerce.users.domain.model.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users", schema = "users_schema")
public class UserEntity {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private Instant updatedAt;

    protected UserEntity() {
    }

    static UserEntity fromDomain(User user) {
        UserEntity e = new UserEntity();
        e.id = user.id() == null ? UUID.randomUUID() : user.id();
        e.email = user.email();
        e.passwordHash = user.passwordHash();
        e.firstName = user.firstName();
        e.lastName = user.lastName();
        e.role = user.role();
        e.active = user.active();
        return e;
    }

    User toDomain() {
        return new User(id, email, passwordHash, firstName, lastName, role, active, createdAt, updatedAt);
    }
}

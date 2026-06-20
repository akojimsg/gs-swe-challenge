package com.gsswec.ecommerce.users.infrastructure.persistence;

import com.gsswec.ecommerce.users.application.port.out.Page;
import com.gsswec.ecommerce.users.application.port.out.UserRepository;
import com.gsswec.ecommerce.users.domain.model.User;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
public class UserRepositoryAdapter implements UserRepository {

    private final UserJpaRepository jpa;

    public UserRepositoryAdapter(UserJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public User save(User user) {
        return jpa.save(UserEntity.fromDomain(user)).toDomain();
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return jpa.findByEmail(email).map(UserEntity::toDomain);
    }

    @Override
    public Optional<User> findById(UUID id) {
        return jpa.findById(id).map(UserEntity::toDomain);
    }

    @Override
    public Page<User> findAll(int page, int size) {
        var result = jpa.findAll(PageRequest.of(page, size, Sort.by("createdAt").descending()));
        List<User> content = result.map(UserEntity::toDomain).getContent();
        return new Page<>(content, page, size, result.getTotalElements(), result.getTotalPages());
    }

    @Override
    public boolean existsByEmail(String email) {
        return jpa.existsByEmail(email);
    }
}

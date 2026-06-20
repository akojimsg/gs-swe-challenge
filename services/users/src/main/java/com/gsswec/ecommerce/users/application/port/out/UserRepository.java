package com.gsswec.ecommerce.users.application.port.out;

import com.gsswec.ecommerce.users.domain.model.User;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository {

    User save(User user);

    Optional<User> findByEmail(String email);

    Optional<User> findById(UUID id);

    Page<User> findAll(int page, int size);

    boolean existsByEmail(String email);
}

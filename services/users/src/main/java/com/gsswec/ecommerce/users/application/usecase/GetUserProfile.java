package com.gsswec.ecommerce.users.application.usecase;

import com.gsswec.ecommerce.users.application.port.out.UserRepository;
import com.gsswec.ecommerce.users.domain.exception.UserNotFoundException;
import com.gsswec.ecommerce.users.domain.model.User;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetUserProfile {

    private final UserRepository users;

    public GetUserProfile(UserRepository users) {
        this.users = users;
    }

    @Transactional(readOnly = true)
    public User byId(UUID userId) {
        return users.findById(userId).orElseThrow(UserNotFoundException::new);
    }
}

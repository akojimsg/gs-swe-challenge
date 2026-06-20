package com.gsswec.ecommerce.users.application.usecase;

import com.gsswec.ecommerce.users.application.port.out.Page;
import com.gsswec.ecommerce.users.application.port.out.UserRepository;
import com.gsswec.ecommerce.users.domain.model.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListUsers {

    private final UserRepository users;

    public ListUsers(UserRepository users) {
        this.users = users;
    }

    @Transactional(readOnly = true)
    public Page<User> list(int page, int size) {
        return users.findAll(page, size);
    }
}

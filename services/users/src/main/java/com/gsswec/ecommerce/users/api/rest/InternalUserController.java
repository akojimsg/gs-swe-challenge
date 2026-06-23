package com.gsswec.ecommerce.users.api.rest;

import com.gsswec.ecommerce.users.api.rest.dto.UserEmailResponse;
import com.gsswec.ecommerce.users.application.usecase.GetUserProfile;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Service-to-service lookups, reachable only on the private network (not routed by
// the gateway, which proxies /api/** only). Lets other services resolve a user's
// email from an id without holding a user-scoped admin token.
@RestController
@RequestMapping("/internal/users")
@Tag(name = "Internal", description = "Service-to-service endpoints (private network only)")
public class InternalUserController {

    private final GetUserProfile getUserProfile;

    public InternalUserController(GetUserProfile getUserProfile) {
        this.getUserProfile = getUserProfile;
    }

    @GetMapping("/{id}/email")
    @Operation(summary = "Resolve a user's email by id (internal)")
    public UserEmailResponse emailById(@PathVariable UUID id) {
        return UserEmailResponse.from(getUserProfile.byId(id));
    }
}

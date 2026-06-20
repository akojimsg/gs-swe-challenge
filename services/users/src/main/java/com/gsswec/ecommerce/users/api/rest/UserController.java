package com.gsswec.ecommerce.users.api.rest;

import com.gsswec.ecommerce.users.api.rest.dto.ChangeRoleRequest;
import com.gsswec.ecommerce.users.api.rest.dto.PagedResponse;
import com.gsswec.ecommerce.users.api.rest.dto.UpdateProfileRequest;
import com.gsswec.ecommerce.users.api.rest.dto.UserResponse;
import com.gsswec.ecommerce.users.application.usecase.ChangeUserRole;
import com.gsswec.ecommerce.users.application.usecase.GetUserProfile;
import com.gsswec.ecommerce.users.application.usecase.ListUsers;
import com.gsswec.ecommerce.users.application.usecase.UpdateUserProfile;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users", description = "Self-service profile and admin user management")
public class UserController {

    private final GetUserProfile getUserProfile;
    private final UpdateUserProfile updateUserProfile;
    private final ListUsers listUsers;
    private final ChangeUserRole changeUserRole;

    public UserController(
            GetUserProfile getUserProfile,
            UpdateUserProfile updateUserProfile,
            ListUsers listUsers,
            ChangeUserRole changeUserRole) {
        this.getUserProfile = getUserProfile;
        this.updateUserProfile = updateUserProfile;
        this.listUsers = listUsers;
        this.changeUserRole = changeUserRole;
    }

    @GetMapping("/me")
    @Operation(summary = "Get the authenticated user's profile")
    public UserResponse me(Principal principal) {
        return UserResponse.from(getUserProfile.byId(UUID.fromString(principal.getName())));
    }

    @PatchMapping("/me")
    @Operation(summary = "Update the authenticated user's profile")
    public UserResponse updateMe(Principal principal, @Valid @RequestBody UpdateProfileRequest request) {
        return UserResponse.from(updateUserProfile.update(
                UUID.fromString(principal.getName()),
                new UpdateUserProfile.Command(request.firstName(), request.lastName())));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List all users (paginated) [ADMIN]")
    public PagedResponse<UserResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return PagedResponse.from(listUsers.list(page, size), UserResponse::from);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get a user by id [ADMIN]")
    public UserResponse getById(@PathVariable UUID id) {
        return UserResponse.from(getUserProfile.byId(id));
    }

    @PatchMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Change a user's role; publishes user.role_changed [ADMIN]")
    public UserResponse changeRole(@PathVariable UUID id, @Valid @RequestBody ChangeRoleRequest request) {
        return UserResponse.from(changeUserRole.change(id, request.role()));
    }
}

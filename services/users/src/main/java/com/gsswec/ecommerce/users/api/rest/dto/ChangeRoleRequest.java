package com.gsswec.ecommerce.users.api.rest.dto;

import com.gsswec.ecommerce.users.domain.model.Role;
import jakarta.validation.constraints.NotNull;

public record ChangeRoleRequest(
        @NotNull Role role) {
}

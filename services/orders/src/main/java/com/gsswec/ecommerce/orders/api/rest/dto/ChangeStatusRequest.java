package com.gsswec.ecommerce.orders.api.rest.dto;

import com.gsswec.ecommerce.orders.domain.model.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record ChangeStatusRequest(@NotNull OrderStatus status) {
}

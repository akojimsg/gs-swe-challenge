package com.gsswec.ecommerce.orders.api.rest.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record PlaceOrderRequest(
        @NotEmpty @Valid List<Line> items) {

    public record Line(
            @NotNull UUID productId,
            @Min(1) int quantity) {
    }
}

package com.gsswec.ecommerce.orders.api.rest.dto;

import java.util.List;

public record ErrorResponse(String error, String message, List<Object> details) {

    public static ErrorResponse of(String error, String message) {
        return new ErrorResponse(error, message, List.of());
    }
}

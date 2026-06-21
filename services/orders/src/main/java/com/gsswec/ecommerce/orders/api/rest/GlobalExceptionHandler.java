package com.gsswec.ecommerce.orders.api.rest;

import com.gsswec.ecommerce.orders.api.rest.dto.ErrorResponse;
import com.gsswec.ecommerce.orders.domain.exception.IllegalStatusTransitionException;
import com.gsswec.ecommerce.orders.domain.exception.InsufficientStockException;
import com.gsswec.ecommerce.orders.domain.exception.OrderNotFoundException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientStock(InsufficientStockException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(
                "insufficient_stock", e.getMessage(), List.copyOf(e.shortfalls())));
    }

    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(OrderNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of("order_not_found", e.getMessage()));
    }

    @ExceptionHandler(IllegalStatusTransitionException.class)
    public ResponseEntity<ErrorResponse> handleIllegalTransition(IllegalStatusTransitionException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of("illegal_status_transition", e.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse.of("forbidden", "Insufficient privileges"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        String detail = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .orElse("Validation failed");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of("validation_failed", detail));
    }

    @ExceptionHandler(org.springframework.web.bind.MissingRequestHeaderException.class)
    public ResponseEntity<ErrorResponse> handleMissingHeader(
            org.springframework.web.bind.MissingRequestHeaderException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of("missing_header", e.getHeaderName() + " is required"));
    }
}

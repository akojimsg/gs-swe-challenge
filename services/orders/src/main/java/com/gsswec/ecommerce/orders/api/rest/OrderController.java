package com.gsswec.ecommerce.orders.api.rest;

import com.gsswec.ecommerce.orders.api.rest.dto.OrderResponse;
import com.gsswec.ecommerce.orders.api.rest.dto.PlaceOrderRequest;
import com.gsswec.ecommerce.orders.application.usecase.PlaceOrder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders")
@Tag(name = "Orders", description = "Order placement and lifecycle")
@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")
public class OrderController {

    private final PlaceOrder placeOrder;

    public OrderController(PlaceOrder placeOrder) {
        this.placeOrder = placeOrder;
    }

    @PostMapping
    @PreAuthorize("hasRole('BUYER')")
    @Operation(summary = "Place an order [BUYER]; idempotent on the Idempotency-Key header")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Order placed; order.placed published"),
        @ApiResponse(responseCode = "409", description = "Insufficient stock", content = @io.swagger.v3.oas.annotations.media.Content),
        @ApiResponse(responseCode = "400", description = "Missing Idempotency-Key or invalid body", content = @io.swagger.v3.oas.annotations.media.Content)
    })
    public ResponseEntity<OrderResponse> place(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody PlaceOrderRequest request,
            Principal principal) {
        UUID userId = UUID.fromString(principal.getName());
        var command = new PlaceOrder.Command(userId, idempotencyKey,
                request.items().stream()
                        .map(i -> new PlaceOrder.Line(i.productId(), i.quantity()))
                        .toList());
        var order = placeOrder.place(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(OrderResponse.from(order));
    }
}

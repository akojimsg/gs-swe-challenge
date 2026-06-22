package com.gsswec.ecommerce.orders.api.rest;

import com.gsswec.ecommerce.orders.api.rest.dto.ChangeStatusRequest;
import com.gsswec.ecommerce.orders.api.rest.dto.OrderResponse;
import com.gsswec.ecommerce.orders.api.rest.dto.PlaceOrderRequest;
import com.gsswec.ecommerce.orders.application.usecase.GetOrders;
import com.gsswec.ecommerce.orders.application.usecase.ManageOrder;
import com.gsswec.ecommerce.orders.application.usecase.PlaceOrder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
    private final GetOrders getOrders;
    private final ManageOrder manageOrder;

    public OrderController(PlaceOrder placeOrder, GetOrders getOrders, ManageOrder manageOrder) {
        this.placeOrder = placeOrder;
        this.getOrders = getOrders;
        this.manageOrder = manageOrder;
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

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List orders — own for a BUYER, all for an ADMIN")
    public List<OrderResponse> list(Authentication auth) {
        List<com.gsswec.ecommerce.orders.domain.model.Order> result = isAdmin(auth)
                ? getOrders.all()
                : getOrders.forUser(UUID.fromString(auth.getName()));
        return result.stream().map(OrderResponse::from).toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get an order by id (own for a BUYER, any for an ADMIN)")
    public OrderResponse getById(@PathVariable UUID id, Authentication auth) {
        return OrderResponse.from(
                getOrders.forCaller(id, UUID.fromString(auth.getName()), isAdmin(auth)));
    }

    @DeleteMapping("/{id}/cancel")
    @PreAuthorize("hasRole('BUYER')")
    @Operation(summary = "Cancel an order while PENDING [BUYER]; publishes order.cancelled")
    public OrderResponse cancel(@PathVariable UUID id, Principal principal) {
        return OrderResponse.from(manageOrder.cancel(id, UUID.fromString(principal.getName())));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Change an order's status [ADMIN]; enforces the lifecycle state machine")
    public OrderResponse changeStatus(@PathVariable UUID id, @Valid @RequestBody ChangeStatusRequest request) {
        return OrderResponse.from(manageOrder.changeStatus(id, request.status()));
    }

    private static boolean isAdmin(Authentication auth) {
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}

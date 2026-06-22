package com.gsswec.ecommerce.orders.application.usecase;

import com.gsswec.ecommerce.orders.application.port.out.OrderRepository;
import com.gsswec.ecommerce.orders.domain.exception.OrderNotFoundException;
import com.gsswec.ecommerce.orders.domain.model.Order;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetOrders {

    private final OrderRepository orders;

    public GetOrders(OrderRepository orders) {
        this.orders = orders;
    }

    @Transactional(readOnly = true)
    public List<Order> forUser(UUID userId) {
        return orders.findByUserId(userId);
    }

    @Transactional(readOnly = true)
    public List<Order> all() {
        return orders.findAll();
    }

    // A buyer may read only their own order; an admin may read any. The caller passes
    // its own id and whether it holds the admin role.
    @Transactional(readOnly = true)
    public Order forCaller(UUID orderId, UUID callerId, boolean isAdmin) {
        Order order = orders.findById(orderId).orElseThrow(OrderNotFoundException::new);
        if (!isAdmin && !order.ownedBy(callerId)) {
            throw new AccessDeniedException("Not the owner of this order");
        }
        return order;
    }
}

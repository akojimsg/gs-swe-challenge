package com.gsswec.ecommerce.notifications.application.port.out;

import java.util.Optional;
import java.util.UUID;

// Resolves a user's email from their id. Saga events (order/payment) and
// user.role_changed carry only a userId, so the recipient is looked up here.
// Returns empty when the address can't be resolved, so the caller degrades
// gracefully rather than failing the notification.
public interface UserDirectory {

    Optional<String> emailFor(UUID userId);
}

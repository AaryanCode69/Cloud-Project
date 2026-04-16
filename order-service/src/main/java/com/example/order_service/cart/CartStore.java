package com.example.order_service.cart;

import java.util.Optional;
import java.util.UUID;

public interface CartStore {
    CartSnapshot addItem(UUID userId, UUID productId, int quantity);

    Optional<CartSnapshot> findByUserId(UUID userId);

    void clear(UUID userId);
}

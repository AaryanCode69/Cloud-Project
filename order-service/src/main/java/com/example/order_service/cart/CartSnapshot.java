package com.example.order_service.cart;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CartSnapshot(
        UUID userId,
        List<CartItemSnapshot> items,
        Instant updatedAt
) {
}

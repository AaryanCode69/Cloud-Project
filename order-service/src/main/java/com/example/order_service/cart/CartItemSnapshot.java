package com.example.order_service.cart;

import java.util.UUID;

public record CartItemSnapshot(
        UUID productId,
        Integer quantity
) {
}

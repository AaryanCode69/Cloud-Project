package com.example.order_service.dto;

import java.util.UUID;

public record CartItemResponseDTO(
        UUID productId,
        Integer quantity
) {
}


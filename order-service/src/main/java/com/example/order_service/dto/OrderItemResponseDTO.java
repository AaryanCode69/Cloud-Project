package com.example.order_service.dto;

import java.util.UUID;

public record OrderItemResponseDTO(
        UUID productId,
        Integer quantity,
        Double price
) {
}


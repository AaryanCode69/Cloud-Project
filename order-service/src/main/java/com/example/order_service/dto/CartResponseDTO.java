package com.example.order_service.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CartResponseDTO(
        UUID userId,
        List<CartItemResponseDTO> items,
        Instant updatedAt
) {
}


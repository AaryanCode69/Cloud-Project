package com.example.order_service.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderResponseDTO(
        UUID id,
        UUID userId,
        String status,
        Double totalAmount,
        Instant createdAt,
        List<OrderItemResponseDTO> items
) {
}


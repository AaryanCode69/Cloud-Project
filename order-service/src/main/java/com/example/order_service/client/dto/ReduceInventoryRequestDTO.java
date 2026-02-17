package com.example.order_service.client.dto;

import java.util.UUID;

public record ReduceInventoryRequestDTO(
        UUID productId,
        Integer quantity
) {
}


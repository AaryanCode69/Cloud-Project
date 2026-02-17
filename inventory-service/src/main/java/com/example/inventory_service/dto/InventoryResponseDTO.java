package com.example.inventory_service.dto;

import java.time.Instant;
import java.util.UUID;

public record InventoryResponseDTO(
        UUID id,
        UUID productId,
        Integer quantityAvailable,
        Instant lastUpdated
) {
}


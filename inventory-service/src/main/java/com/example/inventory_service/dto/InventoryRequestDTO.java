package com.example.inventory_service.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.UUID;

public record InventoryRequestDTO(
        @NotNull UUID productId,
        @NotNull @Positive Integer quantityAvailable
) {
}


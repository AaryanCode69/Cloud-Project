package com.example.order_service.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.UUID;

public record CartItemRequestDTO(
        @NotNull UUID productId,
        @NotNull @Positive Integer quantity
) {
}


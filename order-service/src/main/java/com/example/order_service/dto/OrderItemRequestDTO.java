package com.example.order_service.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.UUID;

public record OrderItemRequestDTO(
        @NotNull UUID productId,
        @NotNull @Positive Integer quantity,
        @NotNull @Positive Double price
) {
}


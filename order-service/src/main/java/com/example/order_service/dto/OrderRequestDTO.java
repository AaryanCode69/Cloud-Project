package com.example.order_service.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;
import java.util.UUID;

public record OrderRequestDTO(
        @NotNull UUID userId,
        @NotNull @Valid List<OrderItemRequestDTO> items,
        @NotNull @Positive Double totalAmount
) {
}


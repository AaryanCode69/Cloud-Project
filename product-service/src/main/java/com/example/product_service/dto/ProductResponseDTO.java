package com.example.product_service.dto;

import java.time.Instant;
import java.util.UUID;

public record ProductResponseDTO(
        UUID id,
        String name,
        String description,
        Double price,
        String category,
        Instant createdAt
) {
}


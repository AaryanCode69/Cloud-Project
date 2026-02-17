package com.example.order_service.client.dto;

import java.util.UUID;

public record ProductClientResponseDTO(
        UUID id,
        String name,
        Double price,
        String category
) {
}


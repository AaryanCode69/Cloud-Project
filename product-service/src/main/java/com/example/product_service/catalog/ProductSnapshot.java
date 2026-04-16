package com.example.product_service.catalog;

import java.time.Instant;
import java.util.UUID;

public record ProductSnapshot(
        UUID id,
        String name,
        String description,
        Double price,
        String category,
        Instant createdAt
) {
}

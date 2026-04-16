package com.example.product_service.catalog;

public record ProductDraft(
        String name,
        String description,
        Double price,
        String category
) {
}

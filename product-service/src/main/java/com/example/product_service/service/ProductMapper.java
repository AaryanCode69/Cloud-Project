package com.example.product_service.service;

import com.example.product_service.catalog.ProductDraft;
import com.example.product_service.catalog.ProductSnapshot;
import com.example.product_service.cosmos.CosmosProductDocument;
import com.example.product_service.dto.ProductRequestDTO;
import com.example.product_service.dto.ProductResponseDTO;
import com.example.product_service.entity.Product;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class ProductMapper {

    public ProductDraft toDraft(ProductRequestDTO request) {
        return new ProductDraft(
                request.name().trim(),
                normalizeOptionalText(request.description()),
                request.price(),
                normalizeOptionalText(request.category())
        );
    }

    public ProductResponseDTO toResponse(ProductSnapshot product) {
        return new ProductResponseDTO(
                product.id(),
                product.name(),
                product.description(),
                product.price(),
                product.category(),
                product.createdAt()
        );
    }

    public ProductSnapshot toSnapshot(Product product) {
        return new ProductSnapshot(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getCategory(),
                product.getCreatedAt()
        );
    }

    public ProductSnapshot toSnapshot(CosmosProductDocument document) {
        return new ProductSnapshot(
                UUID.fromString(document.getId()),
                document.getName(),
                document.getDescription(),
                document.getPrice(),
                document.getCategory(),
                document.getCreatedAt()
        );
    }

    public Product toNewEntity(ProductDraft draft) {
        Product product = new Product();
        apply(product, draft);
        return product;
    }

    public void apply(Product product, ProductDraft draft) {
        product.setName(draft.name());
        product.setDescription(draft.description());
        product.setPrice(draft.price());
        product.setCategory(draft.category());
    }

    public CosmosProductDocument toNewDocument(ProductDraft draft) {
        CosmosProductDocument document = CosmosProductDocument.builder()
                .id(UUID.randomUUID().toString())
                .createdAt(Instant.now())
                .build();
        apply(document, draft);
        return document;
    }

    public void apply(CosmosProductDocument document, ProductDraft draft) {
        document.setName(draft.name());
        document.setDescription(draft.description());
        document.setPrice(draft.price());
        document.setCategory(draft.category());
    }

    private String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }
}

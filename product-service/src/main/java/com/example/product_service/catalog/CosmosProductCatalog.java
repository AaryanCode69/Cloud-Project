package com.example.product_service.catalog;

import com.example.product_service.cosmos.CosmosCatalogService;
import com.example.product_service.cosmos.CosmosProductDocument;
import com.example.product_service.exception.ResourceNotFoundException;
import com.example.product_service.service.ProductMapper;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "feature", name = "cosmos-catalog-enabled", havingValue = "true")
public class CosmosProductCatalog implements ProductCatalog {
    private final CosmosCatalogService cosmosCatalogService;
    private final ProductMapper productMapper;

    @Override
    public ProductSnapshot create(ProductDraft draft) {
        CosmosProductDocument saved = cosmosCatalogService.save(productMapper.toNewDocument(draft));
        return productMapper.toSnapshot(saved);
    }

    @Override
    public ProductSnapshot update(UUID id, ProductDraft draft) {
        CosmosProductDocument existing = cosmosCatalogService.findById(id.toString())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        productMapper.apply(existing, draft);
        CosmosProductDocument saved = cosmosCatalogService.save(existing);
        return productMapper.toSnapshot(saved);
    }

    @Override
    public ProductSnapshot getById(UUID id) {
        return cosmosCatalogService.findById(id.toString())
                .map(productMapper::toSnapshot)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
    }

    @Override
    public List<ProductSnapshot> getAll() {
        return cosmosCatalogService.findAll().stream()
                .map(productMapper::toSnapshot)
                .toList();
    }
}

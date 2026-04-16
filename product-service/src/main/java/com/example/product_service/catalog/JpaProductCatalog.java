package com.example.product_service.catalog;

import com.example.product_service.entity.Product;
import com.example.product_service.exception.ResourceNotFoundException;
import com.example.product_service.repository.ProductRepository;
import com.example.product_service.service.ProductMapper;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "feature", name = "cosmos-catalog-enabled", havingValue = "false", matchIfMissing = true)
public class JpaProductCatalog implements ProductCatalog {
    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    @Override
    public ProductSnapshot create(ProductDraft draft) {
        Product saved = productRepository.save(productMapper.toNewEntity(draft));
        return productMapper.toSnapshot(saved);
    }

    @Override
    public ProductSnapshot update(UUID id, ProductDraft draft) {
        Product existing = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        productMapper.apply(existing, draft);
        Product saved = productRepository.save(existing);
        return productMapper.toSnapshot(saved);
    }

    @Override
    public ProductSnapshot getById(UUID id) {
        return productRepository.findById(id)
                .map(productMapper::toSnapshot)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
    }

    @Override
    public List<ProductSnapshot> getAll() {
        return productRepository.findAll().stream()
                .map(productMapper::toSnapshot)
                .toList();
    }
}

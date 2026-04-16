package com.example.product_service.search;

import com.example.product_service.catalog.ProductCatalog;
import com.example.product_service.catalog.ProductSnapshot;
import com.example.product_service.dto.ProductResponseDTO;
import com.example.product_service.service.ProductMapper;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "feature", name = "aisearch-enabled", havingValue = "false", matchIfMissing = true)
public class CatalogBackedProductSearchClient implements ProductSearchClient {
    private final ProductCatalog productCatalog;
    private final ProductMapper productMapper;

    public CatalogBackedProductSearchClient(ProductCatalog productCatalog, ProductMapper productMapper) {
        this.productCatalog = productCatalog;
        this.productMapper = productMapper;
    }

    @Override
    public void upsert(ProductResponseDTO product) {
        // Search indexing is not needed when local catalog search is active.
    }

    @Override
    public List<ProductResponseDTO> search(String query) {
        String normalizedQuery = query == null ? "" : query.trim().toLowerCase();
        if (normalizedQuery.isBlank()) {
            return List.of();
        }

        return productCatalog.getAll().stream()
                .filter(product -> containsIgnoreCase(product.name(), normalizedQuery)
                        || containsIgnoreCase(product.description(), normalizedQuery)
                        || containsIgnoreCase(product.category(), normalizedQuery))
                .map(productMapper::toResponse)
                .toList();
    }

    private boolean containsIgnoreCase(String value, String query) {
        return value != null && value.toLowerCase().contains(query);
    }
}

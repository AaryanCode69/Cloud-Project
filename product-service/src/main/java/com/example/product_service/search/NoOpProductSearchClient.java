package com.example.product_service.search;

import com.example.product_service.dto.ProductResponseDTO;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "feature", name = "aisearch-enabled", havingValue = "false", matchIfMissing = true)
public class NoOpProductSearchClient implements ProductSearchClient {

    @Override
    public void upsert(ProductResponseDTO product) {
        // No-op when AI Search is disabled.
    }

    @Override
    public List<ProductResponseDTO> search(String query) {
        return List.of();
    }
}


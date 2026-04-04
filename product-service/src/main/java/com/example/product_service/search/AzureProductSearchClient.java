package com.example.product_service.search;

import com.azure.core.util.Context;
import com.azure.search.documents.SearchClient;
import com.azure.search.documents.SearchDocument;
import com.azure.search.documents.indexes.models.IndexDocumentsBatch;
import com.azure.search.documents.models.SearchOptions;
import com.azure.search.documents.models.SearchResult;
import com.azure.search.documents.util.SearchPagedIterable;
import com.example.product_service.dto.ProductResponseDTO;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "feature", name = "aisearch-enabled", havingValue = "true")
public class AzureProductSearchClient implements ProductSearchClient {
    private static final Logger log = LoggerFactory.getLogger(AzureProductSearchClient.class);

    private final SearchClient searchClient;

    public AzureProductSearchClient(SearchClient searchClient) {
        this.searchClient = searchClient;
    }

    @Override
    public void upsert(ProductResponseDTO product) {
        try {
            SearchDocument document = new SearchDocument();
            document.put("id", product.id().toString());
            document.put("name", product.name());
            document.put("description", product.description());
            document.put("category", product.category());
            document.put("price", product.price());
            document.put("createdAt", product.createdAt() == null ? null : product.createdAt().toString());

            IndexDocumentsBatch<SearchDocument> batch = new IndexDocumentsBatch<>();
            batch.addMergeOrUploadActions(List.of(document));
            searchClient.indexDocuments(batch);
            log.info("Indexed product in Azure AI Search with id={}", product.id());
        } catch (Exception ex) {
            // Prototype mode: indexing failure should not break product creation/update.
            log.error("Failed to index product id={} in Azure AI Search: {}", product.id(), ex.getMessage(), ex);
        }
    }

    @Override
    public List<ProductResponseDTO> search(String query) {
        SearchOptions options = new SearchOptions().setTop(20);
        SearchPagedIterable results = searchClient.search(query, options, Context.NONE);

        return results.stream()
                .map(result -> result.getDocument(SearchDocument.class))
                .map(this::toResponse)
                .toList();
    }

    private ProductResponseDTO toResponse(SearchDocument document) {
        UUID id = UUID.fromString((String) document.get("id"));
        String name = (String) document.get("name");
        String description = (String) document.get("description");
        String category = (String) document.get("category");

        Double price = document.get("price") == null
                ? null
                : ((Number) document.get("price")).doubleValue();

        Instant createdAt = null;
        Object createdAtRaw = document.get("createdAt");
        if (createdAtRaw instanceof String createdAtString && !createdAtString.isBlank()) {
            createdAt = Instant.parse(createdAtString);
        }

        return new ProductResponseDTO(id, name, description, price, category, createdAt);
    }
}




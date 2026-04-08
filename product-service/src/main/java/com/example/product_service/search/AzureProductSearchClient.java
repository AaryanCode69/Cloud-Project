package com.example.product_service.search;

import com.azure.core.util.Context;
import com.azure.search.documents.SearchClient;
import com.azure.search.documents.SearchDocument;
import com.azure.search.documents.indexes.models.IndexDocumentsBatch;
import com.azure.search.documents.models.QueryAnswer;
import com.azure.search.documents.models.QueryAnswerType;
import com.azure.search.documents.models.QueryCaption;
import com.azure.search.documents.models.QueryCaptionType;
import com.azure.search.documents.models.QueryType;
import com.azure.search.documents.models.SearchOptions;
import com.azure.search.documents.models.SearchMode;
import com.azure.search.documents.models.SemanticErrorMode;
import com.azure.search.documents.models.SemanticSearchOptions;
import com.azure.search.documents.util.SearchPagedIterable;
import com.example.product_service.config.SearchProperties;
import com.example.product_service.dto.ProductResponseDTO;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
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
    private static final String[] SEARCH_FIELDS = {"name", "description", "category"};

    private final SearchClient searchClient;
    private final SearchProperties searchProperties;

    public AzureProductSearchClient(SearchClient searchClient, SearchProperties searchProperties) {
        this.searchClient = searchClient;
        this.searchProperties = searchProperties;
    }

    @Override
    public void upsert(ProductResponseDTO product) {
        try {
            SearchDocument document = new SearchDocument();
            document.put("id", product.id().toString());
            document.put("name", product.name());
            document.put("description", product.description());
            document.put("category", product.category());
            document.put("price", product.price() == null ? null : product.price().toString());
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
        String normalizedQuery = query == null ? "" : query.trim();
        if (normalizedQuery.isBlank()) {
            return List.of();
        }

        LinkedHashMap<UUID, ProductResponseDTO> combinedResults = new LinkedHashMap<>();

        if (searchProperties.isSemanticEnabled()) {
            executeSearch(normalizedQuery, buildSemanticOptions(normalizedQuery), combinedResults);
        }

        executeSearch(normalizedQuery, buildKeywordOptions(), combinedResults);

        if (searchProperties.isFuzzyEnabled()) {
            String fuzzyQuery = toFuzzyQuery(normalizedQuery);
            if (!fuzzyQuery.isBlank()) {
                executeSearch(fuzzyQuery, buildFuzzyOptions(), combinedResults);
            }
        }

        return combinedResults.values().stream()
                .limit(searchProperties.getTop())
                .toList();
    }

    private void executeSearch(String query, SearchOptions options, LinkedHashMap<UUID, ProductResponseDTO> combinedResults) {
        try {
            SearchPagedIterable results = searchClient.search(query, options, Context.NONE);
            results.stream()
                    .map(result -> result.getDocument(SearchDocument.class))
                    .map(this::toResponse)
                    .forEach(product -> combinedResults.putIfAbsent(product.id(), product));
        } catch (Exception ex) {
            log.warn("Azure AI Search query failed for '{}': {}", query, ex.getMessage());
        }
    }

    private SearchOptions buildKeywordOptions() {
        return baseOptions()
                .setQueryType(QueryType.SIMPLE)
                .setSearchMode(SearchMode.ANY);
    }

    private SearchOptions buildFuzzyOptions() {
        return baseOptions()
                .setQueryType(QueryType.FULL)
                .setSearchMode(SearchMode.ALL);
    }

    private SearchOptions buildSemanticOptions(String query) {
        return baseOptions()
                .setQueryType(QueryType.SEMANTIC)
                .setSearchMode(SearchMode.ANY)
                .setSemanticSearchOptions(new SemanticSearchOptions()
                        .setSemanticConfigurationName(searchProperties.getSemanticConfigurationName())
                        .setSemanticQuery(query)
                        .setErrorMode(SemanticErrorMode.PARTIAL)
                        .setMaxWaitDuration(Duration.ofSeconds(2))
                        .setQueryCaption(new QueryCaption(QueryCaptionType.EXTRACTIVE).setHighlightEnabled(true))
                        .setQueryAnswer(new QueryAnswer(QueryAnswerType.EXTRACTIVE).setCount(3)));
    }

    private SearchOptions baseOptions() {
        return new SearchOptions()
                .setTop(searchProperties.getTop())
                .setSearchFields(SEARCH_FIELDS)
                .setIncludeTotalCount(false);
    }

    private String toFuzzyQuery(String query) {
        return List.of(query.split("\\s+")).stream()
                .map(this::sanitizeToken)
                .filter(token -> !token.isBlank())
                .map(token -> token + "~")
                .reduce((left, right) -> left + " AND " + right)
                .orElse("");
    }

    private String sanitizeToken(String token) {
        return token.replaceAll("[^\\p{Alnum}]", "").toLowerCase();
    }

    private ProductResponseDTO toResponse(SearchDocument document) {
        UUID id = UUID.fromString((String) document.get("id"));
        String name = (String) document.get("name");
        String description = (String) document.get("description");
        String category = (String) document.get("category");

        Double price = null;
        Object priceRaw = document.get("price");
        if (priceRaw instanceof Number numberValue) {
            price = numberValue.doubleValue();
        } else if (priceRaw instanceof String stringValue && !stringValue.isBlank()) {
            try {
                price = Double.parseDouble(stringValue);
            } catch (NumberFormatException ignored) {
                price = null;
            }
        }

        Instant createdAt = null;
        Object createdAtRaw = document.get("createdAt");
        if (createdAtRaw instanceof String createdAtString && !createdAtString.isBlank()) {
            createdAt = Instant.parse(createdAtString);
        }

        return new ProductResponseDTO(id, name, description, price, category, createdAt);
    }
}


package com.example.product_service.search;

import com.azure.core.exception.HttpResponseException;
import com.azure.search.documents.indexes.SearchIndexClient;
import com.azure.search.documents.indexes.models.SearchField;
import com.azure.search.documents.indexes.models.SearchFieldDataType;
import com.azure.search.documents.indexes.models.SearchIndex;
import com.azure.search.documents.indexes.models.SemanticConfiguration;
import com.azure.search.documents.indexes.models.SemanticField;
import com.azure.search.documents.indexes.models.SemanticPrioritizedFields;
import com.azure.search.documents.indexes.models.SemanticSearch;
import com.example.product_service.config.SearchProperties;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "feature", name = "aisearch-enabled", havingValue = "true")
public class AzureSearchIndexInitializer implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(AzureSearchIndexInitializer.class);

    private final SearchIndexClient searchIndexClient;
    private final SearchProperties searchProperties;

    public AzureSearchIndexInitializer(SearchIndexClient searchIndexClient, SearchProperties searchProperties) {
        this.searchIndexClient = searchIndexClient;
        this.searchProperties = searchProperties;
    }

    @Override
    public void run(org.springframework.boot.ApplicationArguments args) {
        SearchIndex index = getOrCreateIndex();
        if (searchProperties.isSemanticEnabled()) {
            ensureSemanticConfiguration(index);
        }
    }

    private SearchIndex getOrCreateIndex() {
        try {
            SearchIndex existing = searchIndexClient.getIndex(searchProperties.getIndexName());
            log.info("Azure AI Search index '{}' already exists", searchProperties.getIndexName());
            return existing;
        } catch (HttpResponseException ex) {
            if (ex.getResponse() == null || ex.getResponse().getStatusCode() != 404) {
                throw ex;
            }
            SearchIndex created = searchIndexClient.createIndex(buildProductIndex());
            log.info("Created Azure AI Search index '{}'", searchProperties.getIndexName());
            return created;
        }
    }

    private void ensureSemanticConfiguration(SearchIndex index) {
        SemanticSearch semanticSearch = index.getSemanticSearch();
        String configurationName = searchProperties.getSemanticConfigurationName();

        boolean alreadyConfigured = semanticSearch != null
                && semanticSearch.getConfigurations() != null
                && semanticSearch.getConfigurations().stream()
                .anyMatch(configuration -> configurationName.equals(configuration.getName()));

        if (alreadyConfigured) {
            log.info("Azure AI Search semantic configuration '{}' already exists", configurationName);
            return;
        }

        SearchIndex updated = index.setSemanticSearch(new SemanticSearch()
                .setDefaultConfigurationName(configurationName)
                .setConfigurations(List.of(new SemanticConfiguration(
                        configurationName,
                        new SemanticPrioritizedFields()
                                .setTitleField(new SemanticField("name"))
                                .setContentFields(new SemanticField("description"))
                                .setKeywordsFields(new SemanticField("category"))
                ))));

        searchIndexClient.createOrUpdateIndex(updated);
        log.info("Enabled semantic configuration '{}' for Azure AI Search index '{}'",
                configurationName, searchProperties.getIndexName());
    }

    private SearchIndex buildProductIndex() {
        return new SearchIndex(searchProperties.getIndexName())
                .setFields(
                        new SearchField("id", SearchFieldDataType.STRING)
                                .setKey(true)
                                .setFilterable(true)
                                .setSortable(true),
                        new SearchField("name", SearchFieldDataType.STRING)
                                .setSearchable(true)
                                .setFilterable(true)
                                .setSortable(true),
                        new SearchField("description", SearchFieldDataType.STRING)
                                .setSearchable(true),
                        new SearchField("category", SearchFieldDataType.STRING)
                                .setSearchable(true)
                                .setFilterable(true)
                                .setFacetable(true)
                                .setSortable(true),
                        new SearchField("price", SearchFieldDataType.DOUBLE)
                                .setFilterable(true)
                                .setSortable(true)
                                .setFacetable(true),
                        new SearchField("createdAt", SearchFieldDataType.STRING)
                );
    }
}

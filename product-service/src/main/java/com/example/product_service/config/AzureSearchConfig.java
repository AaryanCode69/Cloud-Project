package com.example.product_service.config;

import com.azure.core.credential.AzureKeyCredential;
import com.azure.search.documents.SearchClient;
import com.azure.search.documents.SearchClientBuilder;
import com.azure.search.documents.indexes.SearchIndexClient;
import com.azure.search.documents.indexes.SearchIndexClientBuilder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(SearchProperties.class)
@ConditionalOnProperty(prefix = "feature", name = "aisearch-enabled", havingValue = "true")
public class AzureSearchConfig {

    @Bean
    public SearchClient productSearchClient(SearchProperties properties) {
        return new SearchClientBuilder()
                .endpoint(properties.getEndpoint())
                .credential(new AzureKeyCredential(properties.getApiKey()))
                .indexName(properties.getIndexName())
                .buildClient();
    }

    @Bean
    public SearchIndexClient productSearchIndexClient(SearchProperties properties) {
        return new SearchIndexClientBuilder()
                .endpoint(properties.getEndpoint())
                .credential(new AzureKeyCredential(properties.getApiKey()))
                .buildClient();
    }
}

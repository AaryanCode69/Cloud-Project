package com.example.product_service.config;

import com.azure.core.credential.AzureKeyCredential;
import com.azure.search.documents.SearchClient;
import com.azure.search.documents.SearchClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "feature", name = "aisearch-enabled", havingValue = "true")
public class AzureSearchConfig {

    @Bean
    public SearchClient productSearchClient(
            @Value("${search.endpoint}") String endpoint,
            @Value("${search.api-key}") String apiKey,
            @Value("${search.index-name}") String indexName
    ) {
        return new SearchClientBuilder()
                .endpoint(endpoint)
                .credential(new AzureKeyCredential(apiKey))
                .indexName(indexName)
                .buildClient();
    }
}


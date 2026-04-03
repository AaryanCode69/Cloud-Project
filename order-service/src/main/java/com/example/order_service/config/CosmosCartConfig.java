package com.example.order_service.config;

import com.azure.cosmos.CosmosClient;
import com.azure.cosmos.CosmosClientBuilder;
import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.CosmosDatabase;
import com.azure.cosmos.models.CosmosContainerProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "feature", name = "cosmos-cart-enabled", havingValue = "true")
public class CosmosCartConfig {

    @Bean(destroyMethod = "close")
    public CosmosClient cosmosClient(
            @Value("${cosmos.endpoint}") String endpoint,
            @Value("${cosmos.key}") String key
    ) {
        return new CosmosClientBuilder()
                .endpoint(endpoint)
                .key(key)
                .buildClient();
    }

    @Bean
    public CosmosContainer cartContainer(
            CosmosClient cosmosClient,
            @Value("${cosmos.database}") String databaseName,
            @Value("${cosmos.cart-container}") String containerName
    ) {
        cosmosClient.createDatabaseIfNotExists(databaseName);
        CosmosDatabase database = cosmosClient.getDatabase(databaseName);
        CosmosContainerProperties properties = new CosmosContainerProperties(containerName, "/userId");
        database.createContainerIfNotExists(properties);
        return database.getContainer(containerName);
    }
}


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
            @Value("${cosmos.endpoint:${COSMOS_URI:}}") String endpoint,
            @Value("${cosmos.key:${COSMOS_KEY:}}") String key
    ) {
        return new CosmosClientBuilder()
                .endpoint(endpoint)
                .key(key)
            .gatewayMode()
                .buildClient();
    }

    @Bean
    public CosmosContainer cartContainer(
            CosmosClient cosmosClient,
            @Value("${cosmos.database:${COSMOS_DB_ORDER:}}") String databaseName,
            @Value("${cosmos.cart-container:${COSMOS_CART_CONTAINER:cart}}") String containerName
    ) {
        CosmosContainerProperties properties = new CosmosContainerProperties(containerName, "/userId");
        RuntimeException lastError = null;

        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                cosmosClient.createDatabaseIfNotExists(databaseName);
                CosmosDatabase database = cosmosClient.getDatabase(databaseName);
                database.createContainerIfNotExists(properties);
                return database.getContainer(containerName);
            } catch (RuntimeException ex) {
                lastError = ex;
                if (attempt == 3) {
                    throw ex;
                }
                try {
                    Thread.sleep(1000L * attempt);
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                    throw ex;
                }
            }
        }

        throw lastError;
    }
}


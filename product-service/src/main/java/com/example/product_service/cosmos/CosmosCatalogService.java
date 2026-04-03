package com.example.product_service.cosmos;

import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.models.CosmosItemResponse;
import com.azure.cosmos.models.CosmosQueryRequestOptions;
import com.azure.cosmos.models.PartitionKey;
import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CosmosCatalogService {
    private final CosmosContainer catalogContainer;

    public CosmosProductDocument save(CosmosProductDocument document) {
        CosmosItemResponse<CosmosProductDocument> response = catalogContainer.upsertItem(document);
        return response.getItem();
    }

    public Optional<CosmosProductDocument> findById(String id) {
        try {
            CosmosItemResponse<CosmosProductDocument> response = catalogContainer.readItem(
                    id,
                    new PartitionKey(id),
                    CosmosProductDocument.class
            );
            return Optional.ofNullable(response.getItem());
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    public List<CosmosProductDocument> findAll() {
        return StreamSupport.stream(
                        catalogContainer.queryItems(
                                        "SELECT * FROM c",
                                        new CosmosQueryRequestOptions(),
                                        CosmosProductDocument.class)
                                .iterableByPage()
                                .spliterator(),
                        false)
                .flatMap(page -> page.getResults().stream())
                .toList();
    }
}


package com.example.order_service.cosmos;

import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.models.CosmosItemResponse;
import com.azure.cosmos.models.PartitionKey;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "feature", name = "cosmos-cart-enabled", havingValue = "true")
@RequiredArgsConstructor
public class CosmosCartService {
    private final CosmosContainer cartContainer;

    public CosmosCartDocument addItem(UUID userId, UUID productId, int quantity) {
        CosmosCartDocument cart = findByUserId(userId).orElseGet(() -> newCart(userId));

        cart.getItems().stream()
                .filter(item -> item.getProductId().equals(productId.toString()))
                .findFirst()
                .ifPresentOrElse(
                        item -> item.setQuantity(item.getQuantity() + quantity),
                        () -> cart.getItems().add(CosmosCartItemDocument.builder()
                                .productId(productId.toString())
                                .quantity(quantity)
                                .build())
                );

        cart.setUpdatedAt(Instant.now());
        return upsert(cart);
    }

    public Optional<CosmosCartDocument> findByUserId(UUID userId) {
        try {
            CosmosItemResponse<CosmosCartDocument> response = cartContainer.readItem(
                    userId.toString(),
                    new PartitionKey(userId.toString()),
                    CosmosCartDocument.class
            );
            return Optional.ofNullable(response.getItem());
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    public void clear(UUID userId) {
        findByUserId(userId).ifPresent(cart -> cartContainer.deleteItem(
                cart.getId(),
                new PartitionKey(cart.getUserId()),
                null
        ));
    }

    private CosmosCartDocument upsert(CosmosCartDocument cart) {
        return cartContainer.upsertItem(cart).getItem();
    }

    private CosmosCartDocument newCart(UUID userId) {
        return CosmosCartDocument.builder()
                .id(userId.toString())
                .userId(userId.toString())
                .updatedAt(Instant.now())
                .build();
    }
}


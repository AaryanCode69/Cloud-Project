package com.example.order_service.cosmos;

import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.models.CosmosItemResponse;
import com.azure.cosmos.models.PartitionKey;
import com.example.order_service.cart.CartMapper;
import com.example.order_service.cart.CartSnapshot;
import com.example.order_service.cart.CartStore;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "feature", name = "cosmos-cart-enabled", havingValue = "true")
@RequiredArgsConstructor
public class CosmosCartService implements CartStore {
    private final CosmosContainer cartContainer;
    private final CartMapper cartMapper;

    @Override
    public CartSnapshot addItem(UUID userId, UUID productId, int quantity) {
        CosmosCartDocument cart = findDocumentByUserId(userId).orElseGet(() -> newCart(userId));

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
        return cartMapper.toSnapshot(upsert(cart));
    }

    @Override
    public Optional<CartSnapshot> findByUserId(UUID userId) {
        return findDocumentByUserId(userId).map(cartMapper::toSnapshot);
    }

    @Override
    public void clear(UUID userId) {
        findDocumentByUserId(userId).ifPresent(cart -> cartContainer.deleteItem(
                cart.getId(),
                new PartitionKey(cart.getUserId()),
                null
        ));
    }

    private Optional<CosmosCartDocument> findDocumentByUserId(UUID userId) {
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

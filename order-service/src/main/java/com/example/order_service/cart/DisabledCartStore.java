package com.example.order_service.cart;

import com.example.order_service.exception.ExternalServiceException;
import java.util.Optional;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "feature", name = "cosmos-cart-enabled", havingValue = "false", matchIfMissing = true)
public class DisabledCartStore implements CartStore {

    @Override
    public CartSnapshot addItem(UUID userId, UUID productId, int quantity) {
        throw serviceDisabled();
    }

    @Override
    public Optional<CartSnapshot> findByUserId(UUID userId) {
        throw serviceDisabled();
    }

    @Override
    public void clear(UUID userId) {
        // Cart clearing becomes a no-op when the feature is disabled.
    }

    private ExternalServiceException serviceDisabled() {
        return new ExternalServiceException("Cosmos cart feature is disabled");
    }
}

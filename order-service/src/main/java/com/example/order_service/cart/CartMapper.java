package com.example.order_service.cart;

import com.example.order_service.cosmos.CosmosCartDocument;
import com.example.order_service.dto.CartItemResponseDTO;
import com.example.order_service.dto.CartResponseDTO;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class CartMapper {

    public CartSnapshot toSnapshot(CosmosCartDocument cart) {
        return new CartSnapshot(
                UUID.fromString(cart.getUserId()),
                cart.getItems().stream()
                        .map(item -> new CartItemSnapshot(UUID.fromString(item.getProductId()), item.getQuantity()))
                        .toList(),
                cart.getUpdatedAt()
        );
    }

    public CartResponseDTO toResponse(CartSnapshot cart) {
        List<CartItemResponseDTO> items = cart.items().stream()
                .map(item -> new CartItemResponseDTO(item.productId(), item.quantity()))
                .toList();

        return new CartResponseDTO(cart.userId(), items, cart.updatedAt());
    }
}

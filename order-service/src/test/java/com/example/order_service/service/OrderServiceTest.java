package com.example.order_service.service;

import com.example.order_service.cart.CartItemSnapshot;
import com.example.order_service.cart.CartMapper;
import com.example.order_service.cart.CartSnapshot;
import com.example.order_service.cart.CartStore;
import com.example.order_service.client.InventoryGateway;
import com.example.order_service.client.ProductGateway;
import com.example.order_service.client.dto.ProductClientResponseDTO;
import com.example.order_service.dto.CartItemRequestDTO;
import com.example.order_service.dto.CartResponseDTO;
import com.example.order_service.dto.OrderItemRequestDTO;
import com.example.order_service.dto.OrderRequestDTO;
import com.example.order_service.dto.OrderResponseDTO;
import com.example.order_service.entity.Order;
import com.example.order_service.entity.OrderStatus;
import com.example.order_service.event.OrderEventPublisher;
import com.example.order_service.exception.ResourceNotFoundException;
import com.example.order_service.store.OrderStore;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OrderServiceTest {

    @Test
    void createOrderPersistsPublishesAndClearsCart() {
        UUID userId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        InMemoryOrderStore orderStore = new InMemoryOrderStore();
        StubProductGateway productGateway = new StubProductGateway(Map.of(
                productId,
                new ProductClientResponseDTO(productId, "Laptop", 1299.0, "Electronics")
        ));
        RecordingInventoryGateway inventoryGateway = new RecordingInventoryGateway();
        RecordingOrderEventPublisher publisher = new RecordingOrderEventPublisher();
        RecordingCartStore cartStore = new RecordingCartStore();

        OrderService service = new OrderService(
                orderStore,
                productGateway,
                inventoryGateway,
                publisher,
                cartStore,
                new OrderMapper(),
                new CartMapper()
        );

        OrderResponseDTO response = service.createOrder(new OrderRequestDTO(
                userId,
                List.of(new OrderItemRequestDTO(productId, 2))
        ));

        assertNotNull(response.id());
        assertEquals(OrderStatus.CREATED.name(), response.status());
        assertEquals(2598.0, response.totalAmount());
        assertEquals(List.of(productId + ":2"), inventoryGateway.reductions);
        assertSame(orderStore.savedOrder, publisher.publishedOrder);
        assertEquals(List.of(userId), cartStore.clearedUsers);
    }

    @Test
    void cartOperationsUseCartStoreAbstraction() {
        UUID userId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        RecordingCartStore cartStore = new RecordingCartStore();
        cartStore.currentCart = new CartSnapshot(
                userId,
                List.of(new CartItemSnapshot(productId, 3)),
                Instant.parse("2026-02-02T00:00:00Z")
        );

        OrderService service = new OrderService(
                new InMemoryOrderStore(),
                new StubProductGateway(Map.of()),
                new RecordingInventoryGateway(),
                new RecordingOrderEventPublisher(),
                cartStore,
                new OrderMapper(),
                new CartMapper()
        );

        CartResponseDTO cart = service.getCart(userId);

        assertEquals(userId, cart.userId());
        assertEquals(1, cart.items().size());
        assertEquals(productId, cart.items().getFirst().productId());
        assertThrows(ResourceNotFoundException.class, () -> service.getCart(UUID.randomUUID()));
        assertEquals(userId, service.addCartItem(userId, new CartItemRequestDTO(productId, 1)).userId());
    }

    private static final class InMemoryOrderStore implements OrderStore {
        private Order savedOrder;

        @Override
        public Order save(Order order) {
            if (order.getId() == null) {
                order.setId(UUID.randomUUID());
            }
            if (order.getStatus() == null) {
                order.setStatus(OrderStatus.CREATED);
            }
            if (order.getCreatedAt() == null) {
                order.setCreatedAt(Instant.parse("2026-01-01T00:00:00Z"));
            }
            this.savedOrder = order;
            return order;
        }

        @Override
        public Order getById(UUID id) {
            if (savedOrder == null || !savedOrder.getId().equals(id)) {
                throw new ResourceNotFoundException("Order not found");
            }
            return savedOrder;
        }

        @Override
        public List<Order> getByUserId(UUID userId) {
            if (savedOrder == null || !savedOrder.getUserId().equals(userId)) {
                return List.of();
            }
            return List.of(savedOrder);
        }
    }

    private record StubProductGateway(Map<UUID, ProductClientResponseDTO> products) implements ProductGateway {

        @Override
        public ProductClientResponseDTO getProductById(UUID productId) {
            ProductClientResponseDTO product = products.get(productId);
            if (product == null) {
                throw new ResourceNotFoundException("Product not found");
            }
            return product;
        }
    }

    private static final class RecordingInventoryGateway implements InventoryGateway {
        private final List<String> reductions = new ArrayList<>();

        @Override
        public void reduceInventory(UUID productId, int quantity) {
            reductions.add(productId + ":" + quantity);
        }
    }

    private static final class RecordingOrderEventPublisher implements OrderEventPublisher {
        private Order publishedOrder;

        @Override
        public void publishOrderCreated(Order order) {
            this.publishedOrder = order;
        }
    }

    private static final class RecordingCartStore implements CartStore {
        private final List<UUID> clearedUsers = new ArrayList<>();
        private CartSnapshot currentCart;

        @Override
        public CartSnapshot addItem(UUID userId, UUID productId, int quantity) {
            if (currentCart == null || !currentCart.userId().equals(userId)) {
                currentCart = new CartSnapshot(userId, new ArrayList<>(), Instant.parse("2026-02-02T00:00:00Z"));
            }

            List<CartItemSnapshot> updatedItems = new ArrayList<>(currentCart.items());
            updatedItems.add(new CartItemSnapshot(productId, quantity));
            currentCart = new CartSnapshot(userId, updatedItems, Instant.parse("2026-02-02T00:00:00Z"));
            return currentCart;
        }

        @Override
        public Optional<CartSnapshot> findByUserId(UUID userId) {
            return Optional.ofNullable(currentCart)
                    .filter(cart -> cart.userId().equals(userId));
        }

        @Override
        public void clear(UUID userId) {
            clearedUsers.add(userId);
        }
    }
}

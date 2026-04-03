package com.example.order_service.service;

import com.example.order_service.client.InventoryClient;
import com.example.order_service.client.ProductClient;
import com.example.order_service.client.dto.ProductClientResponseDTO;
import com.example.order_service.cosmos.CosmosCartDocument;
import com.example.order_service.cosmos.CosmosCartService;
import com.example.order_service.dto.CartItemRequestDTO;
import com.example.order_service.dto.CartItemResponseDTO;
import com.example.order_service.dto.CartResponseDTO;
import com.example.order_service.dto.OrderItemResponseDTO;
import com.example.order_service.dto.OrderRequestDTO;
import com.example.order_service.dto.OrderResponseDTO;
import com.example.order_service.entity.Order;
import com.example.order_service.entity.OrderItem;
import com.example.order_service.exception.ExternalServiceException;
import com.example.order_service.exception.ResourceNotFoundException;
import com.example.order_service.repository.OrderRepository;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {
    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final ProductClient productClient;
    private final InventoryClient inventoryClient;
    private final ObjectProvider<CosmosCartService> cosmosCartServiceProvider;
    private final boolean cosmosCartEnabled;

    public OrderService(
            OrderRepository orderRepository,
            ProductClient productClient,
            InventoryClient inventoryClient,
            ObjectProvider<CosmosCartService> cosmosCartServiceProvider,
            @Value("${feature.cosmos-cart-enabled:false}") boolean cosmosCartEnabled
    ) {
        this.orderRepository = orderRepository;
        this.productClient = productClient;
        this.inventoryClient = inventoryClient;
        this.cosmosCartServiceProvider = cosmosCartServiceProvider;
        this.cosmosCartEnabled = cosmosCartEnabled;
    }

    public OrderResponseDTO createOrder(OrderRequestDTO request) {
        List<OrderItem> items = request.items().stream()
                .map(item -> {
                    ProductClientResponseDTO product = productClient.getProductById(item.productId());
                    inventoryClient.reduceInventory(item.productId(), item.quantity());

                    return OrderItem.builder()
                            .productId(item.productId())
                            .quantity(item.quantity())
                            .price(product.price())
                            .build();
                })
                .toList();

        double totalAmount = items.stream()
                .mapToDouble(item -> item.getPrice() * item.getQuantity())
                .sum();

        Order order = Order.builder()
                .userId(request.userId())
                .totalAmount(totalAmount)
                .items(items)
                .build();

        Order saved = orderRepository.save(order);

        if (cosmosCartEnabled) {
            getCosmosCartService().clear(request.userId());
        }

        log.info("Created order with id={} for userId={}", saved.getId(), saved.getUserId());
        return toResponse(saved);
    }

    public CartResponseDTO addCartItem(UUID userId, CartItemRequestDTO request) {
        CosmosCartDocument cart = getCosmosCartService().addItem(userId, request.productId(), request.quantity());
        return toCartResponse(cart);
    }

    public CartResponseDTO getCart(UUID userId) {
        CosmosCartDocument cart = getCosmosCartService().findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found"));
        return toCartResponse(cart);
    }

    public void clearCart(UUID userId) {
        getCosmosCartService().clear(userId);
    }

    @Transactional(readOnly = true)
    public OrderResponseDTO getOrderById(UUID id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        return toResponse(order);
    }

    @Transactional(readOnly = true)
    public List<OrderResponseDTO> getOrdersByUserId(UUID userId) {
        return orderRepository.findByUserId(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    private OrderResponseDTO toResponse(Order order) {
        List<OrderItemResponseDTO> items = order.getItems().stream()
                .map(item -> new OrderItemResponseDTO(
                        item.getProductId(),
                        item.getQuantity(),
                        item.getPrice()))
                .toList();

        return new OrderResponseDTO(
                order.getId(),
                order.getUserId(),
                order.getStatus().name(),
                order.getTotalAmount(),
                order.getCreatedAt(),
                items
        );
    }

    private CosmosCartService getCosmosCartService() {
        if (!cosmosCartEnabled) {
            throw new ExternalServiceException("Cosmos cart feature is disabled");
        }

        CosmosCartService service = cosmosCartServiceProvider.getIfAvailable();
        if (service == null) {
            throw new ExternalServiceException("Cosmos cart feature is enabled but Cosmos client is not configured");
        }

        return service;
    }

    private CartResponseDTO toCartResponse(CosmosCartDocument cart) {
        List<CartItemResponseDTO> items = cart.getItems().stream()
                .map(item -> new CartItemResponseDTO(
                        UUID.fromString(item.getProductId()),
                        item.getQuantity()))
                .toList();

        return new CartResponseDTO(
                UUID.fromString(cart.getUserId()),
                items,
                cart.getUpdatedAt()
        );
    }
}

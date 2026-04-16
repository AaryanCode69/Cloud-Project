package com.example.order_service.service;

import com.example.order_service.cart.CartMapper;
import com.example.order_service.cart.CartSnapshot;
import com.example.order_service.cart.CartStore;
import com.example.order_service.client.InventoryGateway;
import com.example.order_service.client.ProductGateway;
import com.example.order_service.client.dto.ProductClientResponseDTO;
import com.example.order_service.dto.CartItemRequestDTO;
import com.example.order_service.dto.CartResponseDTO;
import com.example.order_service.dto.OrderRequestDTO;
import com.example.order_service.dto.OrderResponseDTO;
import com.example.order_service.entity.Order;
import com.example.order_service.entity.OrderItem;
import com.example.order_service.event.OrderEventPublisher;
import com.example.order_service.exception.ResourceNotFoundException;
import com.example.order_service.store.OrderStore;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {
    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderStore orderStore;
    private final ProductGateway productGateway;
    private final InventoryGateway inventoryGateway;
    private final OrderEventPublisher orderEventPublisher;
    private final CartStore cartStore;
    private final OrderMapper orderMapper;
    private final CartMapper cartMapper;

    public OrderService(
            OrderStore orderStore,
            ProductGateway productGateway,
            InventoryGateway inventoryGateway,
            OrderEventPublisher orderEventPublisher,
            CartStore cartStore,
            OrderMapper orderMapper,
            CartMapper cartMapper
    ) {
        this.orderStore = orderStore;
        this.productGateway = productGateway;
        this.inventoryGateway = inventoryGateway;
        this.orderEventPublisher = orderEventPublisher;
        this.cartStore = cartStore;
        this.orderMapper = orderMapper;
        this.cartMapper = cartMapper;
    }

    @Transactional
    public OrderResponseDTO createOrder(OrderRequestDTO request) {
        List<OrderItem> items = buildOrderItems(request);
        double totalAmount = calculateTotalAmount(items);

        Order order = Order.builder()
                .userId(request.userId())
                .totalAmount(totalAmount)
                .items(items)
                .build();

        Order saved = orderStore.save(order);
        orderEventPublisher.publishOrderCreated(saved);
        cartStore.clear(request.userId());

        log.info("Created order with id={} for userId={}", saved.getId(), saved.getUserId());
        return orderMapper.toResponse(saved);
    }

    public CartResponseDTO addCartItem(UUID userId, CartItemRequestDTO request) {
        CartSnapshot cart = cartStore.addItem(userId, request.productId(), request.quantity());
        return cartMapper.toResponse(cart);
    }

    public CartResponseDTO getCart(UUID userId) {
        CartSnapshot cart = cartStore.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found"));
        return cartMapper.toResponse(cart);
    }

    public void clearCart(UUID userId) {
        cartStore.clear(userId);
    }

    @Transactional(readOnly = true)
    public OrderResponseDTO getOrderById(UUID id) {
        return orderMapper.toResponse(orderStore.getById(id));
    }

    @Transactional(readOnly = true)
    public List<OrderResponseDTO> getOrdersByUserId(UUID userId) {
        return orderStore.getByUserId(userId).stream()
                .map(orderMapper::toResponse)
                .toList();
    }

    private List<OrderItem> buildOrderItems(OrderRequestDTO request) {
        return request.items().stream()
                .map(item -> {
                    ProductClientResponseDTO product = productGateway.getProductById(item.productId());
                    inventoryGateway.reduceInventory(item.productId(), item.quantity());

                    return OrderItem.builder()
                            .productId(item.productId())
                            .quantity(item.quantity())
                            .price(product.price())
                            .build();
                })
                .toList();
    }

    private double calculateTotalAmount(List<OrderItem> items) {
        return items.stream()
                .mapToDouble(item -> item.getPrice() * item.getQuantity())
                .sum();
    }
}

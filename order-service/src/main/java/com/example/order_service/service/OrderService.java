package com.example.order_service.service;

import com.example.order_service.client.InventoryClient;
import com.example.order_service.client.ProductClient;
import com.example.order_service.client.dto.ProductClientResponseDTO;
import com.example.order_service.dto.OrderItemResponseDTO;
import com.example.order_service.dto.OrderRequestDTO;
import com.example.order_service.dto.OrderResponseDTO;
import com.example.order_service.entity.Order;
import com.example.order_service.entity.OrderItem;
import com.example.order_service.exception.ResourceNotFoundException;
import com.example.order_service.repository.OrderRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderService {
    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final ProductClient productClient;
    private final InventoryClient inventoryClient;

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
        log.info("Created order with id={} for userId={}", saved.getId(), saved.getUserId());
        return toResponse(saved);
    }

    public OrderResponseDTO getOrderById(UUID id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        return toResponse(order);
    }

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
}

package com.example.order_service.event;

import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import com.example.order_service.entity.Order;
import com.example.order_service.entity.OrderItem;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "feature", name = "servicebus-enabled", havingValue = "true")
public class ServiceBusOrderEventPublisher implements OrderEventPublisher {
    private static final Logger log = LoggerFactory.getLogger(ServiceBusOrderEventPublisher.class);

    private final ServiceBusSenderClient senderClient;

    public ServiceBusOrderEventPublisher(ServiceBusSenderClient senderClient) {
        this.senderClient = senderClient;
    }

    @Override
    public void publishOrderCreated(Order order) {
        String payload = toOrderCreatedEventJson(order);

        try {
            ServiceBusMessage message = new ServiceBusMessage(payload)
                    .setMessageId(order.getId().toString())
                    .setContentType("application/json")
                    .setSubject("OrderCreatedEvent");

            senderClient.sendMessage(message);
            log.info("Published OrderCreatedEvent for orderId={} and userId={}", order.getId(), order.getUserId());
        } catch (Exception ex) {
            // Prototype mode: keep order creation successful even if event publish fails.
            log.error("Failed to publish OrderCreatedEvent for orderId={}: {}", order.getId(), ex.getMessage(), ex);
        }
    }

    private String toOrderCreatedEventJson(Order order) {
        String itemsJson = order.getItems().stream()
                .map(this::toItemJson)
                .collect(Collectors.joining(","));

        return "{" +
                "\"orderId\":\"" + escape(order.getId().toString()) + "\"," +
                "\"userId\":\"" + escape(order.getUserId().toString()) + "\"," +
                "\"totalAmount\":" + order.getTotalAmount() + "," +
                "\"createdAt\":\"" + escape(order.getCreatedAt().toString()) + "\"," +
                "\"items\":[" + itemsJson + "]" +
                "}";
    }

    private String toItemJson(OrderItem item) {
        return "{" +
                "\"productId\":\"" + escape(item.getProductId().toString()) + "\"," +
                "\"quantity\":" + item.getQuantity() + "," +
                "\"price\":" + item.getPrice() +
                "}";
    }

    private String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}


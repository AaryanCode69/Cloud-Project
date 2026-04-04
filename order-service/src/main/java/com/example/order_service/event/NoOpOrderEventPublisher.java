package com.example.order_service.event;

import com.example.order_service.entity.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "feature", name = "servicebus-enabled", havingValue = "false", matchIfMissing = true)
public class NoOpOrderEventPublisher implements OrderEventPublisher {
    private static final Logger log = LoggerFactory.getLogger(NoOpOrderEventPublisher.class);

    @Override
    public void publishOrderCreated(Order order) {
        log.debug("Service Bus disabled; skipping OrderCreatedEvent publish for orderId={}", order.getId());
    }
}


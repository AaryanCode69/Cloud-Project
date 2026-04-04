package com.example.order_service.event;

import com.example.order_service.entity.Order;

public interface OrderEventPublisher {
    void publishOrderCreated(Order order);
}


package com.example.order_service.store;

import com.example.order_service.entity.Order;
import java.util.List;
import java.util.UUID;

public interface OrderStore {
    Order save(Order order);

    Order getById(UUID id);

    List<Order> getByUserId(UUID userId);
}

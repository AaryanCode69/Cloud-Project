package com.example.order_service.client;

import java.util.UUID;

public interface InventoryGateway {
    void reduceInventory(UUID productId, int quantity);
}

package com.example.order_service.repository;

import com.example.order_service.entity.OrderItem;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {
}


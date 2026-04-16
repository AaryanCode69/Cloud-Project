package com.example.order_service.client;

import com.example.order_service.client.dto.ProductClientResponseDTO;
import java.util.UUID;

public interface ProductGateway {
    ProductClientResponseDTO getProductById(UUID productId);
}

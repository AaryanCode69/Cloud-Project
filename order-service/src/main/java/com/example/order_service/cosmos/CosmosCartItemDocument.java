package com.example.order_service.cosmos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CosmosCartItemDocument {
    private String productId;
    private Integer quantity;
}


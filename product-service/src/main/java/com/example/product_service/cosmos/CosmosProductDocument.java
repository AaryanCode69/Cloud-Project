package com.example.product_service.cosmos;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CosmosProductDocument {
    private String id;
    private String name;
    private String description;
    private Double price;
    private String category;
    private Instant createdAt;
}


package com.example.order_service.cosmos;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CosmosCartDocument {
    private String id;
    private String userId;

    @Builder.Default
    private List<CosmosCartItemDocument> items = new ArrayList<>();

    private Instant updatedAt;
}


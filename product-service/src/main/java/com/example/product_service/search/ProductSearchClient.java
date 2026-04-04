package com.example.product_service.search;

import com.example.product_service.dto.ProductResponseDTO;
import java.util.List;

public interface ProductSearchClient {
    void upsert(ProductResponseDTO product);

    List<ProductResponseDTO> search(String query);
}


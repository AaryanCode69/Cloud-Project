package com.example.product_service.service;

import com.example.product_service.catalog.ProductCatalog;
import com.example.product_service.catalog.ProductSnapshot;
import com.example.product_service.dto.ProductRequestDTO;
import com.example.product_service.dto.ProductResponseDTO;
import com.example.product_service.search.ProductSearchClient;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ProductService {
    private final ProductCatalog productCatalog;
    private final ProductSearchClient productSearchClient;
    private final ProductMapper productMapper;

    public ProductService(
            ProductCatalog productCatalog,
            ProductSearchClient productSearchClient,
            ProductMapper productMapper
    ) {
        this.productCatalog = productCatalog;
        this.productSearchClient = productSearchClient;
        this.productMapper = productMapper;
    }

    public ProductResponseDTO createProduct(ProductRequestDTO request) {
        ProductSnapshot saved = productCatalog.create(productMapper.toDraft(request));
        ProductResponseDTO response = productMapper.toResponse(saved);
        productSearchClient.upsert(response);
        return response;
    }

    public ProductResponseDTO updateProduct(UUID id, ProductRequestDTO request) {
        ProductSnapshot saved = productCatalog.update(id, productMapper.toDraft(request));
        ProductResponseDTO response = productMapper.toResponse(saved);
        productSearchClient.upsert(response);
        return response;
    }

    public ProductResponseDTO getProductById(UUID id) {
        return productMapper.toResponse(productCatalog.getById(id));
    }

    public List<ProductResponseDTO> getAllProducts() {
        return productCatalog.getAll().stream()
                .map(productMapper::toResponse)
                .toList();
    }

    public List<ProductResponseDTO> searchProducts(String query) {
        return productSearchClient.search(query);
    }
}

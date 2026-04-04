package com.example.product_service.service;

import com.example.product_service.cosmos.CosmosCatalogService;
import com.example.product_service.cosmos.CosmosProductDocument;
import com.example.product_service.dto.ProductRequestDTO;
import com.example.product_service.dto.ProductResponseDTO;
import com.example.product_service.entity.Product;
import com.example.product_service.exception.ResourceNotFoundException;
import com.example.product_service.repository.ProductRepository;
import com.example.product_service.search.ProductSearchClient;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ProductService {
    private static final Logger log = LoggerFactory.getLogger(ProductService.class);

    private final ProductRepository productRepository;
    private final ObjectProvider<CosmosCatalogService> cosmosCatalogServiceProvider;
    private final ProductSearchClient productSearchClient;
    private final boolean cosmosCatalogEnabled;
    private final boolean aiSearchEnabled;

    public ProductService(
            ProductRepository productRepository,
            ObjectProvider<CosmosCatalogService> cosmosCatalogServiceProvider,
            ProductSearchClient productSearchClient,
            @Value("${feature.cosmos-catalog-enabled:false}") boolean cosmosCatalogEnabled,
            @Value("${feature.aisearch-enabled:false}") boolean aiSearchEnabled
    ) {
        this.productRepository = productRepository;
        this.cosmosCatalogServiceProvider = cosmosCatalogServiceProvider;
        this.productSearchClient = productSearchClient;
        this.cosmosCatalogEnabled = cosmosCatalogEnabled;
        this.aiSearchEnabled = aiSearchEnabled;
    }

    public ProductResponseDTO createProduct(ProductRequestDTO request) {
        if (cosmosCatalogEnabled) {
            CosmosProductDocument document = CosmosProductDocument.builder()
                    .id(UUID.randomUUID().toString())
                    .name(request.name())
                    .description(request.description())
                    .price(request.price())
                    .category(request.category())
                    .createdAt(Instant.now())
                    .build();

            CosmosProductDocument saved = getCosmosCatalogService().save(document);
            log.info("Created Cosmos catalog product with id={} and name={}", saved.getId(), saved.getName());
            ProductResponseDTO response = toResponse(saved);
            productSearchClient.upsert(response);
            return response;
        }

        Product product = Product.builder()
                .name(request.name())
                .description(request.description())
                .price(request.price())
                .category(request.category())
                .build();

        Product saved = productRepository.save(product);
        log.info("Created product with id={} and name={}", saved.getId(), saved.getName());
        ProductResponseDTO response = toResponse(saved);
        productSearchClient.upsert(response);
        return response;
    }

    public ProductResponseDTO updateProduct(UUID id, ProductRequestDTO request) {
        if (cosmosCatalogEnabled) {
            CosmosProductDocument existing = getCosmosCatalogService().findById(id.toString())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

            existing.setName(request.name());
            existing.setDescription(request.description());
            existing.setPrice(request.price());
            existing.setCategory(request.category());

            CosmosProductDocument saved = getCosmosCatalogService().save(existing);
            ProductResponseDTO response = toResponse(saved);
            productSearchClient.upsert(response);
            return response;
        }

        Product existing = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        existing.setName(request.name());
        existing.setDescription(request.description());
        existing.setPrice(request.price());
        existing.setCategory(request.category());

        Product saved = productRepository.save(existing);
        ProductResponseDTO response = toResponse(saved);
        productSearchClient.upsert(response);
        return response;
    }

    public ProductResponseDTO getProductById(UUID id) {
        if (cosmosCatalogEnabled) {
            CosmosProductDocument document = getCosmosCatalogService().findById(id.toString())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
            return toResponse(document);
        }

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        return toResponse(product);
    }

    public List<ProductResponseDTO> getAllProducts() {
        if (cosmosCatalogEnabled) {
            return getCosmosCatalogService().findAll().stream()
                    .map(this::toResponse)
                    .toList();
        }

        return productRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public List<ProductResponseDTO> searchProducts(String query) {
        if (aiSearchEnabled) {
            return productSearchClient.search(query);
        }

        String q = query == null ? "" : query.toLowerCase();
        return getAllProducts().stream()
                .filter(product -> containsIgnoreCase(product.name(), q)
                        || containsIgnoreCase(product.description(), q)
                        || containsIgnoreCase(product.category(), q))
                .toList();
    }

    private CosmosCatalogService getCosmosCatalogService() {
        CosmosCatalogService service = cosmosCatalogServiceProvider.getIfAvailable();
        if (service == null) {
            throw new IllegalStateException("Cosmos catalog feature is enabled but CosmosCatalogService is not available");
        }
        return service;
    }

    private ProductResponseDTO toResponse(Product product) {
        return new ProductResponseDTO(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getCategory(),
                product.getCreatedAt()
        );
    }

    private ProductResponseDTO toResponse(CosmosProductDocument document) {
        return new ProductResponseDTO(
                UUID.fromString(document.getId()),
                document.getName(),
                document.getDescription(),
                document.getPrice(),
                document.getCategory(),
                document.getCreatedAt()
        );
    }

    private boolean containsIgnoreCase(String value, String query) {
        return value != null && value.toLowerCase().contains(query);
    }
}


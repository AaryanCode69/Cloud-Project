package com.example.product_service.service;

import com.example.product_service.catalog.ProductCatalog;
import com.example.product_service.catalog.ProductDraft;
import com.example.product_service.catalog.ProductSnapshot;
import com.example.product_service.dto.ProductRequestDTO;
import com.example.product_service.dto.ProductResponseDTO;
import com.example.product_service.search.CatalogBackedProductSearchClient;
import com.example.product_service.search.ProductSearchClient;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductServiceTest {

    @Test
    void createProductUsesCatalogMapperAndIndexer() {
        RecordingProductCatalog catalog = new RecordingProductCatalog();
        RecordingSearchClient searchClient = new RecordingSearchClient();
        ProductMapper mapper = new ProductMapper();
        ProductService service = new ProductService(catalog, searchClient, mapper);

        ProductResponseDTO response = service.createProduct(new ProductRequestDTO(
                "  Laptop  ",
                "  Premium ultrabook  ",
                1499.0,
                "  Electronics  "
        ));

        assertEquals("Laptop", catalog.lastDraft.name());
        assertEquals("Premium ultrabook", catalog.lastDraft.description());
        assertEquals("Electronics", catalog.lastDraft.category());
        assertEquals(response, searchClient.lastUpserted);
        assertNotNull(response.id());
        assertEquals("Laptop", response.name());
        assertEquals("Premium ultrabook", response.description());
    }

    @Test
    void catalogBackedSearchFallsBackToLocalFiltering() {
        ProductMapper mapper = new ProductMapper();
        StaticProductCatalog catalog = new StaticProductCatalog(List.of(
                new ProductSnapshot(
                        UUID.randomUUID(),
                        "Laptop",
                        "Portable workstation",
                        1499.0,
                        "Electronics",
                        Instant.parse("2026-01-01T00:00:00Z")
                ),
                new ProductSnapshot(
                        UUID.randomUUID(),
                        "Desk",
                        "Oak standing desk",
                        599.0,
                        "Furniture",
                        Instant.parse("2026-01-02T00:00:00Z")
                )
        ));

        CatalogBackedProductSearchClient searchClient = new CatalogBackedProductSearchClient(catalog, mapper);

        List<ProductResponseDTO> results = searchClient.search("workstation");

        assertEquals(1, results.size());
        assertEquals("Laptop", results.getFirst().name());
        assertTrue(searchClient.search("   ").isEmpty());
    }

    private static final class RecordingProductCatalog implements ProductCatalog {
        private ProductDraft lastDraft;

        @Override
        public ProductSnapshot create(ProductDraft draft) {
            this.lastDraft = draft;
            return new ProductSnapshot(
                    UUID.randomUUID(),
                    draft.name(),
                    draft.description(),
                    draft.price(),
                    draft.category(),
                    Instant.parse("2026-01-01T00:00:00Z")
            );
        }

        @Override
        public ProductSnapshot update(UUID id, ProductDraft draft) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ProductSnapshot getById(UUID id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<ProductSnapshot> getAll() {
            return List.of();
        }
    }

    private record StaticProductCatalog(List<ProductSnapshot> products) implements ProductCatalog {

        @Override
        public ProductSnapshot create(ProductDraft draft) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ProductSnapshot update(UUID id, ProductDraft draft) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ProductSnapshot getById(UUID id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<ProductSnapshot> getAll() {
            return products;
        }
    }

    private static final class RecordingSearchClient implements ProductSearchClient {
        private ProductResponseDTO lastUpserted;

        @Override
        public void upsert(ProductResponseDTO product) {
            this.lastUpserted = product;
        }

        @Override
        public List<ProductResponseDTO> search(String query) {
            return List.of();
        }
    }
}

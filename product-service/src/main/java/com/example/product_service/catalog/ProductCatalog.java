package com.example.product_service.catalog;

import java.util.List;
import java.util.UUID;

public interface ProductCatalog {
    ProductSnapshot create(ProductDraft draft);

    ProductSnapshot update(UUID id, ProductDraft draft);

    ProductSnapshot getById(UUID id);

    List<ProductSnapshot> getAll();
}

package com.example.inventory_service.service;

import com.example.inventory_service.dto.InventoryRequestDTO;
import com.example.inventory_service.dto.InventoryResponseDTO;
import com.example.inventory_service.entity.Inventory;
import com.example.inventory_service.exception.ResourceNotFoundException;
import com.example.inventory_service.repository.InventoryRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InventoryService {
    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);

    private final InventoryRepository inventoryRepository;

    public InventoryResponseDTO addInventory(InventoryRequestDTO request) {
        Inventory inventory = Inventory.builder()
                .productId(request.productId())
                .quantityAvailable(request.quantityAvailable())
                .build();

        Inventory saved = inventoryRepository.save(inventory);
        log.info("Added inventory for productId={} with quantity={}", saved.getProductId(), saved.getQuantityAvailable());
        return toResponse(saved);
    }

    public InventoryResponseDTO getInventoryByProductId(UUID productId) {
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found for product"));
        return toResponse(inventory);
    }

    private InventoryResponseDTO toResponse(Inventory inventory) {
        return new InventoryResponseDTO(
                inventory.getId(),
                inventory.getProductId(),
                inventory.getQuantityAvailable(),
                inventory.getLastUpdated()
        );
    }
}


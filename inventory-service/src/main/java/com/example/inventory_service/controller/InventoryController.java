package com.example.inventory_service.controller;

import com.example.inventory_service.dto.InventoryRequestDTO;
import com.example.inventory_service.dto.InventoryResponseDTO;
import com.example.inventory_service.dto.ReduceInventoryRequestDTO;
import com.example.inventory_service.dto.ReduceInventoryResponseDTO;
import com.example.inventory_service.service.InventoryService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {
    private final InventoryService inventoryService;

    @PostMapping
    public ResponseEntity<InventoryResponseDTO> addInventory(@Valid @RequestBody InventoryRequestDTO request) {
        InventoryResponseDTO response = inventoryService.addInventory(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{productId}")
    public ResponseEntity<InventoryResponseDTO> getInventoryByProductId(@PathVariable UUID productId) {
        return ResponseEntity.ok(inventoryService.getInventoryByProductId(productId));
    }

    @PutMapping("/reduce")
    public ResponseEntity<ReduceInventoryResponseDTO> reduceInventory(@Valid @RequestBody ReduceInventoryRequestDTO request) {
        return ResponseEntity.ok(inventoryService.reduceInventory(request));
    }
}

package com.sudesh.warehouse_management_system.controller;

import com.sudesh.warehouse_management_system.dto.InventoryRequestDTO;
import com.sudesh.warehouse_management_system.dto.InventoryResponseDTO;
import com.sudesh.warehouse_management_system.dto.StockAdjustmentDTO;
import com.sudesh.warehouse_management_system.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @PostMapping
    public ResponseEntity<InventoryResponseDTO> createInventory(@Valid @RequestBody InventoryRequestDTO request) {
        InventoryResponseDTO created = inventoryService.createInventory(request);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @PostMapping("/restock")
    public ResponseEntity<InventoryResponseDTO> restock(@Valid @RequestBody StockAdjustmentDTO request) {
        return ResponseEntity.ok(inventoryService.restock(request));
    }

    @PostMapping("/deduct")
    public ResponseEntity<InventoryResponseDTO> deductStock(@Valid @RequestBody StockAdjustmentDTO request) {
        return ResponseEntity.ok(inventoryService.deductStock(request));
    }

    @GetMapping
    public ResponseEntity<List<InventoryResponseDTO>> getAllInventory() {
        return ResponseEntity.ok(inventoryService.getAllInventory());
    }

    @GetMapping("/product/{productId}/warehouse/{warehouseId}")
    public ResponseEntity<InventoryResponseDTO> getInventory(
            @PathVariable Long productId,
            @PathVariable Long warehouseId) {
        return ResponseEntity.ok(inventoryService.getInventoryByProductAndWarehouse(productId, warehouseId));
    }
}
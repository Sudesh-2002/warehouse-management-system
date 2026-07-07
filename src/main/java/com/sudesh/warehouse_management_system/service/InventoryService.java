package com.sudesh.warehouse_management_system.service;

import com.sudesh.warehouse_management_system.dto.InventoryRequestDTO;
import com.sudesh.warehouse_management_system.dto.InventoryResponseDTO;
import com.sudesh.warehouse_management_system.dto.StockAdjustmentDTO;
import com.sudesh.warehouse_management_system.entity.Inventory;
import com.sudesh.warehouse_management_system.entity.Product;
import com.sudesh.warehouse_management_system.entity.Warehouse;
import com.sudesh.warehouse_management_system.exception.DuplicateResourceException;
import com.sudesh.warehouse_management_system.exception.InsufficientStockException;
import com.sudesh.warehouse_management_system.exception.ResourceNotFoundException;
import com.sudesh.warehouse_management_system.repository.InventoryRepository;
import com.sudesh.warehouse_management_system.repository.ProductRepository;
import com.sudesh.warehouse_management_system.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;

    /**
     * Initializes stock for a product in a warehouse.
     * Fails if a record for this product+warehouse combination already exists —
     * use adjustStock (restock) to change an existing quantity instead.
     */
    @Transactional
    public InventoryResponseDTO createInventory(InventoryRequestDTO request) {
        if (inventoryRepository.existsByProductIdAndWarehouseId(request.getProductId(), request.getWarehouseId())) {
            throw new DuplicateResourceException(
                    "Inventory record already exists for this product and warehouse. Use restock instead.");
        }

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + request.getProductId()));

        Warehouse warehouse = warehouseRepository.findById(request.getWarehouseId())
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found with id: " + request.getWarehouseId()));

        Inventory inventory = Inventory.builder()
                .product(product)
                .warehouse(warehouse)
                .quantityOnHand(request.getQuantityOnHand())
                .reservedQuantity(0)
                .build();

        Inventory saved = inventoryRepository.save(inventory);
        return mapToResponse(saved);
    }

    @Transactional
    public InventoryResponseDTO restock(StockAdjustmentDTO request) {
        Inventory inventory = inventoryRepository
                .findByProductIdAndWarehouseIdForUpdate(request.getProductId(), request.getWarehouseId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No inventory record found for this product in this warehouse"));

        inventory.setQuantityOnHand(inventory.getQuantityOnHand() + request.getQuantity());
        Inventory updated = inventoryRepository.save(inventory);
        return mapToResponse(updated);
    }

    @Transactional
    public InventoryResponseDTO deductStock(StockAdjustmentDTO request) {
        Inventory inventory = inventoryRepository
                .findByProductIdAndWarehouseIdForUpdate(request.getProductId(), request.getWarehouseId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No inventory record found for this product in this warehouse"));

        int available = inventory.getQuantityOnHand() - inventory.getReservedQuantity();

        if (available < request.getQuantity()) {
            throw new InsufficientStockException(
                    String.format(
                            "Insufficient stock for product id %d in warehouse id %d. Requested: %d, Available: %d",
                            request.getProductId(), request.getWarehouseId(), request.getQuantity(), available));
        }

        inventory.setQuantityOnHand(inventory.getQuantityOnHand() - request.getQuantity());
        Inventory updated = inventoryRepository.save(inventory);
        return mapToResponse(updated);
    }

    public List<InventoryResponseDTO> getAllInventory() {
        return inventoryRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public InventoryResponseDTO getInventoryByProductAndWarehouse(Long productId, Long warehouseId) {
        Inventory inventory = inventoryRepository.findByProductIdAndWarehouseId(productId, warehouseId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No inventory record found for this product in this warehouse"));
        return mapToResponse(inventory);
    }

    private InventoryResponseDTO mapToResponse(Inventory inventory) {
        int available = inventory.getQuantityOnHand() - inventory.getReservedQuantity();
        return InventoryResponseDTO.builder()
                .id(inventory.getId())
                .productId(inventory.getProduct().getId())
                .productName(inventory.getProduct().getName())
                .warehouseId(inventory.getWarehouse().getId())
                .warehouseName(inventory.getWarehouse().getName())
                .quantityOnHand(inventory.getQuantityOnHand())
                .reservedQuantity(inventory.getReservedQuantity())
                .availableQuantity(available)
                .build();
    }
}
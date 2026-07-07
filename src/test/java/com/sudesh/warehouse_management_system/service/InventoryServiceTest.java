package com.sudesh.warehouse_management_system.service;

import com.sudesh.warehouse_management_system.dto.StockAdjustmentDTO;
import com.sudesh.warehouse_management_system.entity.Inventory;
import com.sudesh.warehouse_management_system.entity.Product;
import com.sudesh.warehouse_management_system.entity.Warehouse;
import com.sudesh.warehouse_management_system.exception.InsufficientStockException;
import com.sudesh.warehouse_management_system.exception.ResourceNotFoundException;
import com.sudesh.warehouse_management_system.repository.InventoryRepository;
import com.sudesh.warehouse_management_system.repository.ProductRepository;
import com.sudesh.warehouse_management_system.repository.WarehouseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private WarehouseRepository warehouseRepository;

    @InjectMocks
    private InventoryService inventoryService;

    private Product product;
    private Warehouse warehouse;
    private Inventory inventory;

    @BeforeEach
    void setUp() {
        product = Product.builder()
                .id(1L)
                .sku("SKU-001")
                .name("Basmati Rice 5kg")
                .category("Grains")
                .unitPrice(BigDecimal.valueOf(12.50))
                .unitOfMeasure("BAG")
                .build();

        warehouse = Warehouse.builder()
                .id(1L)
                .code("WH-COL-01")
                .name("Colombo Main Warehouse")
                .location("Colombo")
                .capacity(10000)
                .build();

        inventory = Inventory.builder()
                .id(1L)
                .product(product)
                .warehouse(warehouse)
                .quantityOnHand(100)
                .reservedQuantity(0)
                .build();
    }

    @Test
    void deductStock_shouldReduceQuantity_whenSufficientStockAvailable() {
        StockAdjustmentDTO request = new StockAdjustmentDTO();
        request.setProductId(1L);
        request.setWarehouseId(1L);
        request.setQuantity(30);

        when(inventoryRepository.findByProductIdAndWarehouseIdForUpdate(1L, 1L))
                .thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var result = inventoryService.deductStock(request);

        assertThat(result.getQuantityOnHand()).isEqualTo(70);
        assertThat(result.getAvailableQuantity()).isEqualTo(70);
        verify(inventoryRepository).save(inventory);
    }

    @Test
    void deductStock_shouldThrowInsufficientStockException_whenRequestExceedsAvailableStock() {
        StockAdjustmentDTO request = new StockAdjustmentDTO();
        request.setProductId(1L);
        request.setWarehouseId(1L);
        request.setQuantity(150); // more than the 100 on hand

        when(inventoryRepository.findByProductIdAndWarehouseIdForUpdate(1L, 1L))
                .thenReturn(Optional.of(inventory));

        assertThatThrownBy(() -> inventoryService.deductStock(request))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("Insufficient stock");

        // Ensure no save happened — stock must remain untouched on failure
        verify(inventoryRepository, never()).save(any(Inventory.class));
    }

    @Test
    void deductStock_shouldAccountForReservedQuantity_whenCheckingAvailability() {
        inventory.setReservedQuantity(80); // 100 on hand, 80 reserved -> only 20 truly available

        StockAdjustmentDTO request = new StockAdjustmentDTO();
        request.setProductId(1L);
        request.setWarehouseId(1L);
        request.setQuantity(25); // exceeds the 20 actually available

        when(inventoryRepository.findByProductIdAndWarehouseIdForUpdate(1L, 1L))
                .thenReturn(Optional.of(inventory));

        assertThatThrownBy(() -> inventoryService.deductStock(request))
                .isInstanceOf(InsufficientStockException.class);
    }

    @Test
    void deductStock_shouldThrowResourceNotFoundException_whenNoInventoryRecordExists() {
        StockAdjustmentDTO request = new StockAdjustmentDTO();
        request.setProductId(99L);
        request.setWarehouseId(1L);
        request.setQuantity(10);

        when(inventoryRepository.findByProductIdAndWarehouseIdForUpdate(99L, 1L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> inventoryService.deductStock(request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void restock_shouldIncreaseQuantityOnHand() {
        StockAdjustmentDTO request = new StockAdjustmentDTO();
        request.setProductId(1L);
        request.setWarehouseId(1L);
        request.setQuantity(50);

        when(inventoryRepository.findByProductIdAndWarehouseIdForUpdate(1L, 1L))
                .thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var result = inventoryService.restock(request);

        assertThat(result.getQuantityOnHand()).isEqualTo(150);
    }
}
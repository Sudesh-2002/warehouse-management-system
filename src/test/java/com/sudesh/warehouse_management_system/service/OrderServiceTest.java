package com.sudesh.warehouse_management_system.service;

import com.sudesh.warehouse_management_system.dto.StockAdjustmentDTO;
import com.sudesh.warehouse_management_system.entity.*;
import com.sudesh.warehouse_management_system.exception.InsufficientStockException;
import com.sudesh.warehouse_management_system.exception.InvalidOrderStatusException;
import com.sudesh.warehouse_management_system.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private com.sudesh.warehouse_management_system.repository.CustomerRepository customerRepository;
    @Mock
    private com.sudesh.warehouse_management_system.repository.ProductRepository productRepository;
    @Mock
    private com.sudesh.warehouse_management_system.repository.WarehouseRepository warehouseRepository;
    @Mock
    private InventoryService inventoryService;
    @Mock
    private com.sudesh.warehouse_management_system.pricing.PricingStrategy pricingStrategy;

    @InjectMocks
    private OrderService orderService;

    private Order pendingOrder;
    private OrderLineItem lineItem;

    @BeforeEach
    void setUp() {
        Product product = Product.builder().id(1L).sku("SKU-001").name("Rice").unitPrice(BigDecimal.TEN).build();
        Warehouse warehouse = Warehouse.builder().id(1L).code("WH-01").name("Main WH").capacity(1000).build();
        Customer customer = Customer.builder().id(1L).name("ABC Restaurant").email("abc@test.com").build();

        lineItem = OrderLineItem.builder()
                .id(1L)
                .product(product)
                .warehouse(warehouse)
                .quantity(20)
                .unitPriceAtOrderTime(BigDecimal.TEN)
                .lineTotal(BigDecimal.valueOf(200))
                .build();

        pendingOrder = Order.builder()
                .id(1L)
                .customer(customer)
                .status(OrderStatus.PENDING)
                .totalAmount(BigDecimal.valueOf(200))
                .lineItems(List.of(lineItem))
                .build();
        lineItem.setOrder(pendingOrder);
    }

    @Test
    void confirmOrder_shouldDeductStockForEachLineItem_andSetStatusToConfirmed() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(pendingOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(inventoryService.deductStock(any(StockAdjustmentDTO.class)))
                .thenReturn(null); // return value not used by confirmOrder

        var result = orderService.confirmOrder(1L);

        assertThat(result.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        verify(inventoryService, times(1)).deductStock(any(StockAdjustmentDTO.class));
    }

    @Test
    void confirmOrder_shouldThrowInvalidOrderStatusException_whenOrderIsNotPending() {
        pendingOrder.setStatus(OrderStatus.SHIPPED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(pendingOrder));

        assertThatThrownBy(() -> orderService.confirmOrder(1L))
                .isInstanceOf(InvalidOrderStatusException.class)
                .hasMessageContaining("Only PENDING orders can be confirmed");

        verify(inventoryService, never()).deductStock(any());
    }

    @Test
    void confirmOrder_shouldPropagateInsufficientStockException_andNotChangeOrderStatus() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(pendingOrder));
        when(inventoryService.deductStock(any(StockAdjustmentDTO.class)))
                .thenThrow(new InsufficientStockException("Insufficient stock for product id 1"));

        assertThatThrownBy(() -> orderService.confirmOrder(1L))
                .isInstanceOf(InsufficientStockException.class);

        // Order should never be saved as CONFIRMED since deduction failed
        verify(orderRepository, never()).save(any(Order.class));
        assertThat(pendingOrder.getStatus()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    void shipOrder_shouldThrowInvalidOrderStatusException_whenOrderIsNotConfirmed() {
        // still PENDING, not CONFIRMED
        when(orderRepository.findById(1L)).thenReturn(Optional.of(pendingOrder));

        assertThatThrownBy(() -> orderService.shipOrder(1L))
                .isInstanceOf(InvalidOrderStatusException.class)
                .hasMessageContaining("Only CONFIRMED orders can be shipped");
    }

    @Test
    void shipOrder_shouldSucceed_whenOrderIsConfirmed() {
        pendingOrder.setStatus(OrderStatus.CONFIRMED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(pendingOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        var result = orderService.shipOrder(1L);

        assertThat(result.getStatus()).isEqualTo(OrderStatus.SHIPPED);
    }

    @Test
    void cancelOrder_shouldThrowInvalidOrderStatusException_whenOrderAlreadyShipped() {
        pendingOrder.setStatus(OrderStatus.SHIPPED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(pendingOrder));

        assertThatThrownBy(() -> orderService.cancelOrder(1L))
                .isInstanceOf(InvalidOrderStatusException.class)
                .hasMessageContaining("Only PENDING orders can be cancelled");
    }
}
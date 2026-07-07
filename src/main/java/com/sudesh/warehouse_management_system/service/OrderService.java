package com.sudesh.warehouse_management_system.service;

import com.sudesh.warehouse_management_system.dto.*;
import com.sudesh.warehouse_management_system.entity.*;
import com.sudesh.warehouse_management_system.exception.InvalidOrderStatusException;
import com.sudesh.warehouse_management_system.exception.ResourceNotFoundException;
import com.sudesh.warehouse_management_system.repository.CustomerRepository;
import com.sudesh.warehouse_management_system.repository.OrderRepository;
import com.sudesh.warehouse_management_system.repository.ProductRepository;
import com.sudesh.warehouse_management_system.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;
    private final InventoryService inventoryService;

    /**
     * Places a new order in PENDING status. No stock is deducted yet —
     * stock is only committed when the order is CONFIRMED.
     */
    @Transactional
    public OrderResponseDTO placeOrder(OrderRequestDTO request) {
        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Customer not found with id: " + request.getCustomerId()));

        Order order = Order.builder()
                .customer(customer)
                .status(OrderStatus.PENDING)
                .build();

        BigDecimal orderTotal = BigDecimal.ZERO;

        for (OrderLineItemRequestDTO lineRequest : request.getLineItems()) {
            Product product = productRepository.findById(lineRequest.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Product not found with id: " + lineRequest.getProductId()));

            Warehouse warehouse = warehouseRepository.findById(lineRequest.getWarehouseId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Warehouse not found with id: " + lineRequest.getWarehouseId()));

            BigDecimal lineTotal = product.getUnitPrice()
                    .multiply(BigDecimal.valueOf(lineRequest.getQuantity()));

            OrderLineItem lineItem = OrderLineItem.builder()
                    .order(order)
                    .product(product)
                    .warehouse(warehouse)
                    .quantity(lineRequest.getQuantity())
                    .unitPriceAtOrderTime(product.getUnitPrice())
                    .lineTotal(lineTotal)
                    .build();

            order.getLineItems().add(lineItem);
            orderTotal = orderTotal.add(lineTotal);
        }

        order.setTotalAmount(orderTotal);

        Order saved = orderRepository.save(order);
        return mapToResponse(saved);
    }

    /**
     * Confirms a PENDING order: deducts stock for every line item.
     * If ANY line item has insufficient stock, the entire operation rolls back
     * (thanks to @Transactional) — no partial stock deduction.
     */
    @Transactional
    public OrderResponseDTO confirmOrder(Long orderId) {
        Order order = getOrderEntity(orderId);

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new InvalidOrderStatusException(
                    "Only PENDING orders can be confirmed. Current status: " + order.getStatus());
        }

        for (OrderLineItem lineItem : order.getLineItems()) {
            StockAdjustmentDTO deductRequest = new StockAdjustmentDTO();
            deductRequest.setProductId(lineItem.getProduct().getId());
            deductRequest.setWarehouseId(lineItem.getWarehouse().getId());
            deductRequest.setQuantity(lineItem.getQuantity());

            // Throws InsufficientStockException if any line item can't be fulfilled —
            // @Transactional ensures earlier deductions in this loop are rolled back too.
            inventoryService.deductStock(deductRequest);
        }

        order.setStatus(OrderStatus.CONFIRMED);
        Order updated = orderRepository.save(order);
        return mapToResponse(updated);
    }

    /**
     * Marks a CONFIRMED order as SHIPPED. Stock was already deducted at confirmation time.
     */
    @Transactional
    public OrderResponseDTO shipOrder(Long orderId) {
        Order order = getOrderEntity(orderId);

        if (order.getStatus() != OrderStatus.CONFIRMED) {
            throw new InvalidOrderStatusException(
                    "Only CONFIRMED orders can be shipped. Current status: " + order.getStatus());
        }

        order.setStatus(OrderStatus.SHIPPED);
        Order updated = orderRepository.save(order);
        return mapToResponse(updated);
    }

    /**
     * Cancels an order. Only allowed while still PENDING (before stock is committed).
     */
    @Transactional
    public OrderResponseDTO cancelOrder(Long orderId) {
        Order order = getOrderEntity(orderId);

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new InvalidOrderStatusException(
                    "Only PENDING orders can be cancelled. Current status: " + order.getStatus());
        }

        order.setStatus(OrderStatus.CANCELLED);
        Order updated = orderRepository.save(order);
        return mapToResponse(updated);
    }

    public List<OrderResponseDTO> getAllOrders() {
        return orderRepository.findAll().stream().map(this::mapToResponse).toList();
    }

    public OrderResponseDTO getOrderById(Long orderId) {
        return mapToResponse(getOrderEntity(orderId));
    }

    private Order getOrderEntity(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));
    }

    private OrderResponseDTO mapToResponse(Order order) {
        List<OrderLineItemResponseDTO> lineItemDTOs = order.getLineItems().stream()
                .map(li -> OrderLineItemResponseDTO.builder()
                        .id(li.getId())
                        .productId(li.getProduct().getId())
                        .productName(li.getProduct().getName())
                        .warehouseId(li.getWarehouse().getId())
                        .warehouseName(li.getWarehouse().getName())
                        .quantity(li.getQuantity())
                        .unitPriceAtOrderTime(li.getUnitPriceAtOrderTime())
                        .lineTotal(li.getLineTotal())
                        .build())
                .toList();

        return OrderResponseDTO.builder()
                .id(order.getId())
                .customerId(order.getCustomer().getId())
                .customerName(order.getCustomer().getName())
                .status(order.getStatus())
                .orderDate(order.getOrderDate())
                .totalAmount(order.getTotalAmount())
                .lineItems(lineItemDTOs)
                .build();
    }
}
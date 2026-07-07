package com.sudesh.warehouse_management_system.controller;

import com.sudesh.warehouse_management_system.dto.OrderRequestDTO;
import com.sudesh.warehouse_management_system.dto.OrderResponseDTO;
import com.sudesh.warehouse_management_system.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderResponseDTO> placeOrder(@Valid @RequestBody OrderRequestDTO request) {
        return new ResponseEntity<>(orderService.placeOrder(request), HttpStatus.CREATED);
    }

    @PostMapping("/{id}/confirm")
    public ResponseEntity<OrderResponseDTO> confirmOrder(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.confirmOrder(id));
    }

    @PostMapping("/{id}/ship")
    public ResponseEntity<OrderResponseDTO> shipOrder(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.shipOrder(id));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<OrderResponseDTO> cancelOrder(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.cancelOrder(id));
    }

    @GetMapping
    public ResponseEntity<List<OrderResponseDTO>> getAllOrders() {
        return ResponseEntity.ok(orderService.getAllOrders());
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponseDTO> getOrderById(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getOrderById(id));
    }
}
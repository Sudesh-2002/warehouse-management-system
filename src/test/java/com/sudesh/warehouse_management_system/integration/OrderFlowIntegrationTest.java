package com.sudesh.warehouse_management_system.integration;

import com.sudesh.warehouse_management_system.dto.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class OrderFlowIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void fullOrderLifecycle_shouldDeductStock_onConfirmation() {
        // 1. Create product
        ProductRequestDTO productRequest = new ProductRequestDTO();
        productRequest.setSku("SKU-FLOW-001");
        productRequest.setName("Flow Test Rice");
        productRequest.setUnitPrice(BigDecimal.valueOf(10.00));
        productRequest.setUnitOfMeasure("BAG");
        ResponseEntity<Map> productResponse =
                restTemplate.postForEntity("/api/products", productRequest, Map.class);
        Long productId = ((Number) productResponse.getBody().get("id")).longValue();

        // 2. Create warehouse
        WarehouseRequestDTO warehouseRequest = new WarehouseRequestDTO();
        warehouseRequest.setCode("WH-FLOW-001");
        warehouseRequest.setName("Flow Test Warehouse");
        warehouseRequest.setLocation("Colombo");
        warehouseRequest.setCapacity(1000);
        ResponseEntity<Map> warehouseResponse =
                restTemplate.postForEntity("/api/warehouses", warehouseRequest, Map.class);
        Long warehouseId = ((Number) warehouseResponse.getBody().get("id")).longValue();

        // 3. Create customer
        CustomerRequestDTO customerRequest = new CustomerRequestDTO();
        customerRequest.setName("Flow Test Customer");
        customerRequest.setEmail("flowtest@customer.com");
        ResponseEntity<Map> customerResponse =
                restTemplate.postForEntity("/api/customers", customerRequest, Map.class);
        Long customerId = ((Number) customerResponse.getBody().get("id")).longValue();

        // 4. Initialize inventory with 100 units
        InventoryRequestDTO inventoryRequest = new InventoryRequestDTO();
        inventoryRequest.setProductId(productId);
        inventoryRequest.setWarehouseId(warehouseId);
        inventoryRequest.setQuantityOnHand(100);
        restTemplate.postForEntity("/api/inventory", inventoryRequest, Map.class);

        // 5. Place an order for 20 units
        OrderLineItemRequestDTO lineItem = new OrderLineItemRequestDTO();
        lineItem.setProductId(productId);
        lineItem.setWarehouseId(warehouseId);
        lineItem.setQuantity(20);

        OrderRequestDTO orderRequest = new OrderRequestDTO();
        orderRequest.setCustomerId(customerId);
        orderRequest.setLineItems(List.of(lineItem));

        ResponseEntity<Map> orderResponse =
                restTemplate.postForEntity("/api/orders", orderRequest, Map.class);
        assertThat(orderResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(orderResponse.getBody().get("status")).isEqualTo("PENDING");
        Long orderId = ((Number) orderResponse.getBody().get("id")).longValue();

        // 6. Confirm the order -> should deduct real stock in the real database
        ResponseEntity<Map> confirmResponse =
                restTemplate.postForEntity("/api/orders/" + orderId + "/confirm", null, Map.class);
        assertThat(confirmResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(confirmResponse.getBody().get("status")).isEqualTo("CONFIRMED");

        // 7. Verify stock was actually deducted in the database (100 - 20 = 80)
        ResponseEntity<Map> inventoryCheck = restTemplate.getForEntity(
                "/api/inventory/product/" + productId + "/warehouse/" + warehouseId, Map.class);
        assertThat(inventoryCheck.getBody().get("quantityOnHand")).isEqualTo(80);

        // 8. Ship the order
        ResponseEntity<Map> shipResponse =
                restTemplate.postForEntity("/api/orders/" + orderId + "/ship", null, Map.class);
        assertThat(shipResponse.getBody().get("status")).isEqualTo("SHIPPED");
    }

    @Test
    void confirmOrder_shouldReturnConflict_whenOrderExceedsAvailableStock() {
        // Create product with only 5 units of stock, then try to order 999
        ProductRequestDTO productRequest = new ProductRequestDTO();
        productRequest.setSku("SKU-FLOW-002");
        productRequest.setName("Low Stock Product");
        productRequest.setUnitPrice(BigDecimal.valueOf(5.00));
        productRequest.setUnitOfMeasure("EACH");
        Long productId = ((Number) restTemplate.postForEntity("/api/products", productRequest, Map.class)
                .getBody().get("id")).longValue();

        WarehouseRequestDTO warehouseRequest = new WarehouseRequestDTO();
        warehouseRequest.setCode("WH-FLOW-002");
        warehouseRequest.setName("Low Stock Warehouse");
        warehouseRequest.setLocation("Kandy");
        warehouseRequest.setCapacity(100);
        Long warehouseId = ((Number) restTemplate.postForEntity("/api/warehouses", warehouseRequest, Map.class)
                .getBody().get("id")).longValue();

        CustomerRequestDTO customerRequest = new CustomerRequestDTO();
        customerRequest.setName("Overselling Test Customer");
        customerRequest.setEmail("oversell@customer.com");
        Long customerId = ((Number) restTemplate.postForEntity("/api/customers", customerRequest, Map.class)
                .getBody().get("id")).longValue();

        InventoryRequestDTO inventoryRequest = new InventoryRequestDTO();
        inventoryRequest.setProductId(productId);
        inventoryRequest.setWarehouseId(warehouseId);
        inventoryRequest.setQuantityOnHand(5); // only 5 in stock
        restTemplate.postForEntity("/api/inventory", inventoryRequest, Map.class);

        OrderLineItemRequestDTO lineItem = new OrderLineItemRequestDTO();
        lineItem.setProductId(productId);
        lineItem.setWarehouseId(warehouseId);
        lineItem.setQuantity(999); // way more than available

        OrderRequestDTO orderRequest = new OrderRequestDTO();
        orderRequest.setCustomerId(customerId);
        orderRequest.setLineItems(List.of(lineItem));

        Long orderId = ((Number) restTemplate.postForEntity("/api/orders", orderRequest, Map.class)
                .getBody().get("id")).longValue();

        ResponseEntity<Map> confirmResponse =
                restTemplate.postForEntity("/api/orders/" + orderId + "/confirm", null, Map.class);

        assertThat(confirmResponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }
}
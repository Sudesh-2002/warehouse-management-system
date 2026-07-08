package com.sudesh.warehouse_management_system.integration;

import com.sudesh.warehouse_management_system.dto.ProductRequestDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class ProductControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void createProduct_shouldPersistToRealDatabase_andReturnCreatedProduct() {
        ProductRequestDTO request = new ProductRequestDTO();
        request.setSku("SKU-IT-001");
        request.setName("Integration Test Rice");
        request.setCategory("Grains");
        request.setUnitPrice(BigDecimal.valueOf(15.00));
        request.setUnitOfMeasure("BAG");

        ResponseEntity<Object> response = restTemplate.postForEntity("/api/products", request, Object.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void createProduct_shouldReturnConflict_whenSkuAlreadyExists() {
        ProductRequestDTO request = new ProductRequestDTO();
        request.setSku("SKU-IT-DUPLICATE");
        request.setName("Duplicate Test Product");
        request.setUnitPrice(BigDecimal.valueOf(10.00));
        request.setUnitOfMeasure("EACH");

        restTemplate.postForEntity("/api/products", request, Object.class); // first call succeeds

        ResponseEntity<Object> secondResponse =
                restTemplate.postForEntity("/api/products", request, Object.class); // duplicate

        assertThat(secondResponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void createProduct_shouldReturnBadRequest_whenValidationFails() {
        ProductRequestDTO request = new ProductRequestDTO(); // all fields blank/null

        ResponseEntity<Object> response = restTemplate.postForEntity("/api/products", request, Object.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
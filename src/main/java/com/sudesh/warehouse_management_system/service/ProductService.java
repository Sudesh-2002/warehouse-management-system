package com.sudesh.warehouse_management_system.service;

import com.sudesh.warehouse_management_system.dto.ProductRequestDTO;
import com.sudesh.warehouse_management_system.dto.ProductResponseDTO;
import com.sudesh.warehouse_management_system.entity.Product;
import com.sudesh.warehouse_management_system.exception.ResourceNotFoundException;
import com.sudesh.warehouse_management_system.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    public ProductResponseDTO createProduct(ProductRequestDTO request) {
        if (productRepository.existsBySku(request.getSku())) {
            throw new IllegalArgumentException("Product with SKU '" + request.getSku() + "' already exists");
        }

        Product product = Product.builder()
                .sku(request.getSku())
                .name(request.getName())
                .category(request.getCategory())
                .unitPrice(request.getUnitPrice())
                .unitOfMeasure(request.getUnitOfMeasure())
                .build();

        Product saved = productRepository.save(product);
        return mapToResponse(saved);
    }

    public List<ProductResponseDTO> getAllProducts() {
        return productRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public ProductResponseDTO getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        return mapToResponse(product);
    }

    public ProductResponseDTO updateProduct(Long id, ProductRequestDTO request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        product.setSku(request.getSku());
        product.setName(request.getName());
        product.setCategory(request.getCategory());
        product.setUnitPrice(request.getUnitPrice());
        product.setUnitOfMeasure(request.getUnitOfMeasure());

        Product updated = productRepository.save(product);
        return mapToResponse(updated);
    }

    public void deleteProduct(Long id) {
        if (!productRepository.existsById(id)) {
            throw new ResourceNotFoundException("Product not found with id: " + id);
        }
        productRepository.deleteById(id);
    }

    private ProductResponseDTO mapToResponse(Product product) {
        return ProductResponseDTO.builder()
                .id(product.getId())
                .sku(product.getSku())
                .name(product.getName())
                .category(product.getCategory())
                .unitPrice(product.getUnitPrice())
                .unitOfMeasure(product.getUnitOfMeasure())
                .build();
    }
}
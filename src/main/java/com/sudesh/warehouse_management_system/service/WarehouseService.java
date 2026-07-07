package com.sudesh.warehouse_management_system.service;

import com.sudesh.warehouse_management_system.dto.WarehouseRequestDTO;
import com.sudesh.warehouse_management_system.dto.WarehouseResponseDTO;
import com.sudesh.warehouse_management_system.entity.Warehouse;
import com.sudesh.warehouse_management_system.exception.DuplicateResourceException;
import com.sudesh.warehouse_management_system.exception.ResourceNotFoundException;
import com.sudesh.warehouse_management_system.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WarehouseService {

    private final WarehouseRepository warehouseRepository;

    public WarehouseResponseDTO createWarehouse(WarehouseRequestDTO request) {
        if (warehouseRepository.existsByCode(request.getCode())) {
            throw new DuplicateResourceException("Warehouse with code '" + request.getCode() + "' already exists");
        }

        Warehouse warehouse = Warehouse.builder()
                .code(request.getCode())
                .name(request.getName())
                .location(request.getLocation())
                .capacity(request.getCapacity())
                .build();

        Warehouse saved = warehouseRepository.save(warehouse);
        return mapToResponse(saved);
    }

    public List<WarehouseResponseDTO> getAllWarehouses() {
        return warehouseRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public WarehouseResponseDTO getWarehouseById(Long id) {
        Warehouse warehouse = warehouseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found with id: " + id));
        return mapToResponse(warehouse);
    }

    public WarehouseResponseDTO updateWarehouse(Long id, WarehouseRequestDTO request) {
        Warehouse warehouse = warehouseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found with id: " + id));

        warehouse.setCode(request.getCode());
        warehouse.setName(request.getName());
        warehouse.setLocation(request.getLocation());
        warehouse.setCapacity(request.getCapacity());

        Warehouse updated = warehouseRepository.save(warehouse);
        return mapToResponse(updated);
    }

    public void deleteWarehouse(Long id) {
        if (!warehouseRepository.existsById(id)) {
            throw new ResourceNotFoundException("Warehouse not found with id: " + id);
        }
        warehouseRepository.deleteById(id);
    }

    private WarehouseResponseDTO mapToResponse(Warehouse warehouse) {
        return WarehouseResponseDTO.builder()
                .id(warehouse.getId())
                .code(warehouse.getCode())
                .name(warehouse.getName())
                .location(warehouse.getLocation())
                .capacity(warehouse.getCapacity())
                .build();
    }
}
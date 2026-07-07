package com.sudesh.warehouse_management_system.repository;

import com.sudesh.warehouse_management_system.entity.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WarehouseRepository extends JpaRepository<Warehouse, Long> {
    boolean existsByCode(String code);
}
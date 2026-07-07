package com.sudesh.warehouse_management_system.repository;

import com.sudesh.warehouse_management_system.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {
}
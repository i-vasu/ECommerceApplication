package com.app.logistics.inventory.repositories;

import com.app.logistics.inventory.entities.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WarehouseRepo extends JpaRepository<Warehouse, Long> {
    List<Warehouse> findByActiveTrue();
}

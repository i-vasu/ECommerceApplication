package com.app.logistics.inventory.repositories;

import com.app.logistics.inventory.entities.Bin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BinRepository extends JpaRepository<Bin, Long> {
    List<Bin> findByWarehouseId(Long warehouseId);
}

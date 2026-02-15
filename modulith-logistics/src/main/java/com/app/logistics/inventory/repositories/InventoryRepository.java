package com.app.logistics.inventory.repositories;

import com.app.logistics.inventory.entities.Inventory;
import com.app.logistics.inventory.entities.InventoryId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, InventoryId> {
    List<Inventory> findByItemCode(String itemCode);
    Optional<Inventory> findByItemCodeAndWarehouseIdAndBinId(String itemCode, Long warehouseId, Long binId);
}

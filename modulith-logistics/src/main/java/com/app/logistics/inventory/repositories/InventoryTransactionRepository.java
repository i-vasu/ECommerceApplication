package com.app.logistics.inventory.repositories;

import com.app.logistics.inventory.entities.InventoryTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, Long> {
    List<InventoryTransaction> findByItemCodeOrderByCreatedAtDesc(String itemCode);
}

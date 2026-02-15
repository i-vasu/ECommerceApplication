package com.app.logistics.inventory.repositories;

import com.app.logistics.inventory.entities.StockMove;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockMoveRepository extends JpaRepository<StockMove, Long> {
    List<StockMove> findByItemCode(String itemCode);
    List<StockMove> findByFromWarehouseIdOrToWarehouseId(Long fromWarehouseId, Long toWarehouseId);
}

package com.app.logistics.inventory.repositories;

import com.app.logistics.inventory.entities.CostLot;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CostLotRepository extends JpaRepository<CostLot, Long> {
    List<CostLot> findByItemCodeOrderByPurchaseDateAsc(String itemCode);
    List<CostLot> findByItemCode(String itemCode);
}

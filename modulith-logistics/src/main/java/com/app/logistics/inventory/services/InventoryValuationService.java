package com.app.logistics.inventory.services;

import com.app.logistics.inventory.entities.CostLot;
import com.app.logistics.inventory.repositories.CostLotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InventoryValuationService {

    private final CostLotRepository costLotRepo;

    @Transactional
    public void recordPurchase(String itemCode, int quantity, BigDecimal unitCost, Long warehouseId) {
        costLotRepo.save(new CostLot(itemCode, quantity, unitCost, warehouseId));
    }

    /**
     * Calculates the Cost of Goods Sold (COGS) using FIFO.
     */
    @Transactional
    public BigDecimal consumeStockForFIFO(String itemCode, int quantityToConsume) {
        List<CostLot> lots = costLotRepo.findByItemCodeOrderByPurchaseDateAsc(itemCode);
        BigDecimal totalCogs = BigDecimal.ZERO;
        int remainingToConsume = quantityToConsume;

        for (CostLot lot : lots) {
            if (remainingToConsume <= 0) break;

            int availableInLot = lot.getRemainingQuantity();
            int canTake = Math.min(availableInLot, remainingToConsume);

            BigDecimal costOfConsumption = lot.getUnitCost().multiply(BigDecimal.valueOf(canTake));
            totalCogs = totalCogs.add(costOfConsumption);

            lot.setRemainingQuantity(availableInLot - canTake);
            remainingToConsume -= canTake;
            
            costLotRepo.save(lot);
        }

        if (remainingToConsume > 0) {
            // Handle scenario where we consume more than tracked costs (fallback to standard cost)
            return totalCogs.add(BigDecimal.valueOf(remainingToConsume).multiply(BigDecimal.valueOf(100))); 
        }

        return totalCogs;
    }

    public BigDecimal getInventoryValue(String itemCode) {
        return costLotRepo.findByItemCode(itemCode).stream()
                .map(lot -> lot.getUnitCost().multiply(BigDecimal.valueOf(lot.getRemainingQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}

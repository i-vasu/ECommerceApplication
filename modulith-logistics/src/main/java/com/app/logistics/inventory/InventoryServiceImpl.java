package com.app.logistics.inventory;

import com.app.logistics.inventory.payloads.InventoryRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class InventoryServiceImpl implements InventoryService {

    private final InventoryReservationService reservationService;

    public InventoryServiceImpl(InventoryReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @Override
    public InventoryLock lockInventory(Long warehouseId, Long binId, String itemCode, int quantity) {
        boolean reserved = reservationService.reserveStock(warehouseId, binId, itemCode, quantity);
        return new InventoryLock(reserved, reserved ? UUID.randomUUID().toString() : null);
    }

    @Override
    public InventoryLock lockFlashInventory(Long productId, int quantity) {
        String lockId = java.util.UUID.randomUUID().toString();
        boolean reserved = reservationService.reserveFlash(productId, quantity, lockId);
        return new InventoryLock(reserved, reserved ? lockId : null);
    }

    @Override
    public InventoryLock lockStock(List<InventoryRequest> requests) {
        String lockId = java.util.UUID.randomUUID().toString();
        boolean allReserved = reservationService.reserveBatch(requests, lockId);
        return new InventoryLock(allReserved, allReserved ? lockId : null);
    }

    @Override
    public void confirmStock(Long warehouseId, Long binId, String itemCode, int quantity, String lockId) {
        reservationService.confirmStock(warehouseId, binId, itemCode, quantity, lockId);
    }

    @Override
    public void confirmFlashStock(Long productId, int quantity, String lockId) {
        reservationService.confirmFlash(productId, quantity, lockId);
    }

    @Override
    public boolean checkAvailability(Long warehouseId, Long binId, String itemCode, int quantity) {
        return reservationService.checkStock(warehouseId, binId, itemCode, quantity);
    }

    @Override
    public boolean checkAggregateAvailability(String itemCode, int quantity) {
        return reservationService.getAggregateStock(itemCode) >= quantity;
    }

    @Override
    public void releaseStock(Long warehouseId, Long binId, String itemCode, int quantity) {
        // Stock release is currently global in Redis for simple restock, 
        // but we'll stick to location-specific restock logic in ReservationService
        reservationService.restock(warehouseId, binId, itemCode, quantity, "Reservation Release");
    }
}

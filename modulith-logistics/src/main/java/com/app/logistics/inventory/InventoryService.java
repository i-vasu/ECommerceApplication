package com.app.logistics.inventory;

import com.app.logistics.inventory.payloads.InventoryRequest;

import java.util.List;

public interface InventoryService {
    InventoryLock lockInventory(Long warehouseId, Long binId, String itemCode, int quantity);

    InventoryLock lockFlashInventory(Long productId, int quantity);

    InventoryLock lockStock(List<InventoryRequest> requests);

    boolean checkAvailability(Long warehouseId, Long binId, String itemCode, int quantity);
    
    boolean checkAggregateAvailability(String itemCode, int quantity);

    void confirmStock(Long warehouseId, Long binId, String itemCode, int quantity, String lockId);

    void confirmFlashStock(Long productId, int quantity, String lockId);

    void releaseStock(Long warehouseId, Long binId, String itemCode, int quantity);

    public record InventoryLock(boolean locked, String lockId) {
    }
}

package com.app.logistics.inventory;

import com.app.logistics.inventory.payloads.InventoryRequest;
import java.util.List;

public interface InventoryService {
    InventoryLock lockInventory(String itemCode, int quantity);

    InventoryLock lockFlashInventory(Long productId, int quantity);

    InventoryLock lockStock(List<InventoryRequest> requests);

    boolean checkAvailability(String itemCode, int quantity);

    void confirmStock(String itemCode, int quantity, String lockId);

    public record InventoryLock(boolean locked, String lockId) {
    }
}

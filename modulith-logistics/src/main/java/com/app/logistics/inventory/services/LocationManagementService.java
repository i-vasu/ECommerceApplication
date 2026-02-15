package com.app.logistics.inventory.services;

import com.app.logistics.inventory.entities.Bin;
import com.app.logistics.inventory.entities.Inventory;
import com.app.logistics.inventory.entities.InventoryId;
import com.app.logistics.inventory.entities.StockMove;
import com.app.logistics.inventory.entities.Warehouse;
import com.app.logistics.inventory.repositories.BinRepository;
import com.app.logistics.inventory.repositories.InventoryRepository;
import com.app.logistics.inventory.repositories.StockMoveRepository;
import com.app.logistics.inventory.repositories.WarehouseRepository;
import com.app.logistics.inventory.InventoryReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LocationManagementService {

    private final WarehouseRepository warehouseRepo;
    private final BinRepository binRepo;
    private final InventoryRepository inventoryRepo;
    private final StockMoveRepository stockMoveRepo;
    private final InventoryReservationService reservationService;

    public Warehouse createWarehouse(String name, String pincode, String city) {
        Warehouse warehouse = new Warehouse();
        warehouse.setName(name);
        warehouse.setPincode(pincode);
        warehouse.setCity(city);
        return warehouseRepo.save(warehouse);
    }

    public Bin createBin(Long warehouseId, String binCode, String zone) {
        Warehouse warehouse = warehouseRepo.findById(warehouseId)
                .orElseThrow(() -> new RuntimeException("Warehouse not found"));
        
        Bin bin = new Bin();
        bin.setWarehouse(warehouse);
        bin.setBinCode(binCode);
        bin.setZone(zone);
        return binRepo.save(bin);
    }

    public List<Warehouse> getAllWarehouses() {
        return warehouseRepo.findAll();
    }

    public List<Bin> getBinsForWarehouse(Long warehouseId) {
        return binRepo.findByWarehouseId(warehouseId);
    }

    @Transactional
    public StockMove executeTransfer(Long fromWarehouseId, Long fromBinId, Long toWarehouseId, Long toBinId, String itemCode, int quantity, String reason, String executedBy) {
        // 1. Verify source inventory
        InventoryId sourceId = new InventoryId(itemCode, fromWarehouseId, fromBinId);
        Inventory sourceInv = inventoryRepo.findById(sourceId)
                .orElseThrow(() -> new RuntimeException("Source inventory not found"));

        if (sourceInv.getQuantity() < quantity) {
            throw new RuntimeException("Insufficient stock in source location");
        }

        // 2. Adjust Source
        reservationService.restock(fromWarehouseId, fromBinId, itemCode, -quantity, "Transfer OUT: " + reason);

        // 3. Adjust Destination
        reservationService.restock(toWarehouseId, toBinId, itemCode, quantity, "Transfer IN: " + reason);

        // 4. Record Move
        StockMove move = new StockMove();
        move.setItemCode(itemCode);
        move.setQuantity(quantity);
        move.setFromWarehouseId(fromWarehouseId);
        move.setFromBinId(fromBinId);
        move.setToWarehouseId(toWarehouseId);
        move.setToBinId(toBinId);
        move.setReason(reason);
        move.setExecutedBy(executedBy);
        
        return stockMoveRepo.save(move);
    }
}

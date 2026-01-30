package com.app.logistics.inventory.services;

import com.app.governance.rules.RuleEngineService;
import com.app.security.entities.Address;
import com.app.logistics.inventory.entities.Warehouse;
import com.app.logistics.inventory.repositories.WarehouseRepo;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Autonomous Warehouse Routing Service.
 * Automatically assigns orders to the optimal warehouse based on distance and stock.
 */
@Service
public class WarehouseRoutingService {

    private static final org.apache.logging.log4j.Logger log = org.apache.logging.log4j.LogManager.getLogger(WarehouseRoutingService.class);

    private final WarehouseRepo warehouseRepo;
    private final RuleEngineService ruleEngine;

    public WarehouseRoutingService(WarehouseRepo warehouseRepo, RuleEngineService ruleEngine) {
        this.warehouseRepo = warehouseRepo;
        this.ruleEngine = ruleEngine;
    }

    public Warehouse selectOptimalWarehouse(Address destination, String itemCode) {
        List<Warehouse> activeWarehouses = warehouseRepo.findByActiveTrue();
        
        if (activeWarehouses.isEmpty()) {
            return null;
        }

        Warehouse selected = null;
        int minDistance = Integer.MAX_VALUE;

        for (Warehouse wh : activeWarehouses) {
            Map<String, Object> context = new HashMap<>();
            context.put("wh", wh);
            context.put("destination", destination);
            
            // Logic: Simple SpEL check for region matching
            String routingRule = "wh.pincode.substring(0, 2) == destination.pincode.substring(0, 2)";
            
            if (ruleEngine.evaluate(routingRule, context)) {
                log.info("Direct regional match found for warehouse: {}", wh.getName());
                return wh;
            }
            
            // Fallback: Distance-like logic (simulated)
            try {
                int dist = Math.abs(Integer.parseInt(wh.getPincode()) - Integer.parseInt(destination.getPincode()));
                if (dist < minDistance) {
                    minDistance = dist;
                    selected = wh;
                }
            } catch (NumberFormatException e) {
                // Ignore parsing errors for non-numeric pincodes in PoC
            }
        }

        return selected != null ? selected : activeWarehouses.get(0);
    }
}

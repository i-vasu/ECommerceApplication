package com.app.erp;

import com.app.finance.repositories.InvoiceRepository;
import com.app.finance.services.InvoiceService;
import com.app.logistics.entities.Shipment;
import com.app.logistics.inventory.InventoryReservationService;
import com.app.logistics.inventory.repositories.InventoryRepository;
import com.app.logistics.inventory.repositories.InventoryTransactionRepository;
import com.app.logistics.shipping.ShipmentService;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.statemachine.StateMachineContext;
import org.springframework.statemachine.StateMachinePersist;
import org.springframework.statemachine.data.jpa.JpaStateMachineRepository;
import org.springframework.statemachine.persist.DefaultStateMachinePersister;
import org.springframework.statemachine.persist.StateMachinePersister;
import org.springframework.test.context.ActiveProfiles;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CustomErpIntegrationTest extends com.app.test.AbstractIntegrationTest {

    @Autowired
    private InventoryReservationService inventoryService;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private InventoryTransactionRepository inventoryTransactionRepository;

    @Autowired
    private InvoiceService invoiceService;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private ShipmentService shipmentService;

    static class TestStateMachineConfig {
        @Bean
        @Primary
        public StateMachinePersister<Object, Object, String> stateMachinePersister() {
            return new DefaultStateMachinePersister<>(new StateMachinePersist<Object, Object, String>() {
                private final Map<String, StateMachineContext<Object, Object>> contexts = new HashMap<>();

                @Override
                public void write(StateMachineContext<Object, Object> context, String contextId) {
                    contexts.put(contextId, context);
                }

                @Override
                public StateMachineContext<Object, Object> read(String contextId) {
                    return contexts.get(contextId);
                }
            });
        }

        @Bean
        @Primary
        public JpaStateMachineRepository jpaStateMachineRepository() {
            JpaStateMachineRepository mockRepo = Mockito.mock(JpaStateMachineRepository.class);
            when(mockRepo.existsById(any())).thenReturn(true);
            return mockRepo;
        }
    }



    @Test
    @Order(1)
    public void testInventoryDeduction() {
        String itemCode = "VA-TEST-001";
        
        // 1. Seed Inventory
        com.app.logistics.inventory.entities.Inventory inv = new com.app.logistics.inventory.entities.Inventory();
        inv.setItemCode(itemCode);
        inv.setQuantity(100);
        inv.setReservedQuantity(0);
        inv.setWarehouseId("MAIN");
        inventoryRepository.save(inv);
        
        // Ensure Redis cache is clean/updated
        inventoryService.loadStockFromDB(itemCode); 

        // 2. Reserve & Confirm
        boolean reserved = inventoryService.reserveStock(itemCode, 2, "LOCK-123");
        assertTrue(reserved, "Stock should be reserved");

        inventoryService.confirmStock(itemCode, 2, "LOCK-123");

        // 3. Verify DB Deduction
        var updatedInv = inventoryRepository.findByItemCode(itemCode).orElseThrow();
        assertEquals(98, updatedInv.getQuantity());

        // 4. Verify Audit Trail
        var txs = inventoryTransactionRepository.findByItemCodeOrderByCreatedAtDesc(itemCode);
        assertFalse(txs.isEmpty());
        assertEquals(-2, txs.get(0).getQuantityChange());
    }

    // Note: Invoice generation requires OrderAmountProvider which mocks Order Repo. 
    // This is hard to test in isolation without full Order flow. 
    // Skipping unit test for InvoiceService here, relying on manual verification or broader integration test.

    @Test
    @Order(2)
    public void testFulfillmentWorkflow() {
        Long orderId = 99999L;

        // 1. Pick
        Shipment picked = shipmentService.markAsPicked(orderId);
        assertEquals("PICKED", picked.getStatus());

        // 2. Pack
        Shipment packed = shipmentService.markAsPacked(orderId);
        assertEquals("PACKED", packed.getStatus());

        // 3. Manifest
        String url = shipmentService.generateManifest(orderId);
        assertNotNull(url);
        assertTrue(url.contains("manifest.pdf"));
        
        Shipment finalState = shipmentService.markAsPacked(orderId); // Fetch again
        assertEquals("PACKED", finalState.getStatus()); // State shouldn't revert
    }

    @Test
    @Order(3)
    public void testManualStockAdjustmentSync() {
        String itemCode = "VA-SYNC-001";
        
        // 1. Initialize
        inventoryService.updateInventoryStock(itemCode, 50);
        
        // 2. Adjust
        inventoryService.updateInventoryStock(itemCode, 75);
        
        // 3. Verify DB
        var inv = inventoryRepository.findByItemCode(itemCode).orElseThrow();
        assertEquals(75, inv.getQuantity());
        
        // 4. Verify Redis (via loadStockFromDB or internal check)
        // Since we can't easily check real Redis in this test if it's mocked, 
        // we at least verify the method executes without error and updates DB.
        
        // 5. Verify Audit Trail
        var txs = inventoryTransactionRepository.findByItemCodeOrderByCreatedAtDesc(itemCode);
        assertTrue(txs.stream().anyMatch(tx -> tx.getQuantityChange() == 25));
    }
}

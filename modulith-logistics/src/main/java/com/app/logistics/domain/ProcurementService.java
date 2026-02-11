package com.app.logistics.domain;

import com.app.logistics.entities.PurchaseOrder;
import com.app.logistics.entities.PurchaseOrderItem;
import com.app.logistics.repositories.PurchaseOrderRepo;
import com.app.logistics.inventory.repositories.InventoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProcurementService {

    private final PurchaseOrderRepo poRepo;
    private final InventoryRepository inventoryRepo;

    @Transactional
    public PurchaseOrder createPO(String supplierName, String supplierEmail, List<PurchaseOrderItem> items) {
        PurchaseOrder po = new PurchaseOrder();
        po.setSupplierName(supplierName);
        po.setSupplierEmail(supplierEmail);
        po.setStatus(PurchaseOrder.POStatus.DRAFT);
        
        double total = 0;
        for (PurchaseOrderItem item : items) {
            item.setPurchaseOrder(po);
            total += (item.getUnitPrice() * item.getQuantity());
        }
        po.setItems(items);
        po.setTotalAmount(total);
        
        return poRepo.save(po);
    }

    @Transactional
    public void receiveGoods(Long poId) {
        PurchaseOrder po = poRepo.findById(poId)
                .orElseThrow(() -> new RuntimeException("Purchase Order not found"));
        
        if (po.getStatus() == PurchaseOrder.POStatus.RECEIVED) {
            throw new RuntimeException("PO already received");
        }

        log.info("Procurement: Receiving goods for PO #{}", poId);

        for (PurchaseOrderItem item : po.getItems()) {
            updateStock(item.getItemCode(), item.getQuantity());
        }

        po.setStatus(PurchaseOrder.POStatus.RECEIVED);
        po.setReceivedAt(LocalDateTime.now());
        poRepo.save(po);
    }

    private void updateStock(String itemCode, Integer quantity) {
        // Integrate with Inventory module logic
        var inventory = inventoryRepo.findByItemCode(itemCode)
                .orElseThrow(() -> new RuntimeException("Inventory record not found for item: " + itemCode));
        
        inventory.setQuantity(inventory.getQuantity() + quantity);
        inventoryRepo.save(inventory);
        
        log.info("Procurement: Updated stock for item {}. New level: {}", itemCode, inventory.getQuantity());
    }
}

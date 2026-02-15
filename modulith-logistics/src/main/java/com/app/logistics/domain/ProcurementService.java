package com.app.logistics.domain;

import com.app.logistics.entities.PurchaseOrder;
import com.app.logistics.entities.PurchaseOrderItem;
import com.app.logistics.repositories.PurchaseOrderRepo;
import com.app.logistics.inventory.repositories.InventoryRepository;
import com.app.finance.services.VendorService;
import com.app.finance.payloads.VendorDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.app.core.events.GoodsReceivedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProcurementService {

    private final PurchaseOrderRepo poRepo;
    private final com.app.logistics.inventory.InventoryReservationService inventoryReservationService;
    private final VendorService vendorService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public PurchaseOrder createPO(Long vendorId, List<PurchaseOrderItem> items) {
        VendorDTO vendor = vendorService.getVendorById(vendorId);
        
        PurchaseOrder po = new PurchaseOrder();
        po.setVendorId(vendor.id());
        po.setSupplierName(vendor.name());
        po.setSupplierEmail(vendor.email());
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
            inventoryReservationService.receiveProcurementStock(item.getItemCode(), item.getQuantity(), "PO#" + poId);
        }

        po.setStatus(PurchaseOrder.POStatus.RECEIVED);
        po.setReceivedAt(LocalDateTime.now());
        poRepo.save(po);

        eventPublisher.publishEvent(new GoodsReceivedEvent(
                po.getId(),
                po.getVendorId(),
                po.getSupplierName(),
                po.getTotalAmount(),
                LocalDateTime.now(),
                po.getItems().stream()
                        .map(i -> new GoodsReceivedEvent.ReceivedItem(i.getItemCode(), i.getQuantity(), i.getUnitPrice()))
                        .toList()
        ));
    }
}

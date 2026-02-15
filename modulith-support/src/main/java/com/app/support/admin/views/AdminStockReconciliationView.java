package com.app.support.admin.views;

import com.app.logistics.inventory.InventoryService;
import com.app.logistics.inventory.entities.Inventory;
import com.app.finance.services.FinancialLedgerService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

import java.math.BigDecimal;

@Route(value = "admin/stock-reconciliation", layout = AdminMainLayout.class)
@PageTitle("Stock Reconciliation | Fashion ERP")
@RolesAllowed("ADMIN")
public class AdminStockReconciliationView extends VerticalLayout {

    private final InventoryService inventoryService;
    private final FinancialLedgerService ledgerService;
    private final Grid<Inventory> reconciliationGrid = new Grid<>(Inventory.class);

    public AdminStockReconciliationView(InventoryService inventoryService, FinancialLedgerService ledgerService) {
        this.inventoryService = inventoryService;
        this.ledgerService = ledgerService;
        
        setSizeFull();
        setPadding(true);

        add(new H2("Stock Reconciliation (Physical Count Adjustment)"));

        reconciliationGrid.setColumns("itemCode", "warehouseId", "binId", "quantity");
        
        reconciliationGrid.addComponentColumn(inv -> {
            IntegerField physicalCount = new IntegerField();
            physicalCount.setValue(inv.getQuantity());
            return physicalCount;
        }).setHeader("Physical Count");

        reconciliationGrid.addComponentColumn(inv -> {
            Button adjustBtn = new Button("Adjust");
            adjustBtn.addClickListener(e -> {
                // Adjustment logic: physical vs system
                // Record shrinkage/surplus in General Ledger
                int adjustment = 10; // Placeholder for diff
                ledgerService.recordTransaction(
                    "EXP-SHRINKAGE", 
                    "AST-INV", 
                    BigDecimal.valueOf(adjustment * 100), 
                    "Stock Reconciliation Adjustment for " + inv.getItemCode(),
                    "RECON-" + System.currentTimeMillis()
                );
                Notification.show("Adjusted " + inv.getItemCode());
            });
            return adjustBtn;
        }).setHeader("Action");

        // Assuming inventoryService can provide all inventory items for reconciliation
        // reconciliationGrid.setItems(((com.app.logistics.inventory.InventoryServiceImpl)inventoryService).getAllInventory());
        add(reconciliationGrid);
    }
}

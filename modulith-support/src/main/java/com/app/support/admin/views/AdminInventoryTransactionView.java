package com.app.support.admin.views;

import com.app.logistics.inventory.repositories.InventoryTransactionRepository;
import com.app.logistics.inventory.entities.InventoryTransaction;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

@Route(value = "admin/inventory/transactions", layout = AdminMainLayout.class)
@PageTitle("Inventory Audit Log | Vasu Admin")
@RolesAllowed("ADMIN")
public class AdminInventoryTransactionView extends VerticalLayout {

    private final InventoryTransactionRepository transactionRepository;
    private final Grid<InventoryTransaction> grid = new Grid<>(InventoryTransaction.class, false);

    public AdminInventoryTransactionView(InventoryTransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
        setSizeFull();
        setPadding(true);

        add(new H1("Inventory Audit Log"));
        configureGrid();
        add(grid);
        updateList();
    }

    private void configureGrid() {
        grid.setSizeFull();
        grid.addColumn(InventoryTransaction::getCreatedAt).setHeader("Timestamp").setSortable(true);
        grid.addColumn(InventoryTransaction::getItemCode).setHeader("Item Code").setSortable(true);
        grid.addColumn(InventoryTransaction::getQuantityChange).setHeader("Change");
        grid.addColumn(InventoryTransaction::getType).setHeader("Type");
        grid.addColumn(InventoryTransaction::getReferenceId).setHeader("Reference");
        grid.addColumn(InventoryTransaction::getReason).setHeader("Reason");
    }

    private void updateList() {
        grid.setItems(transactionRepository.findAll());
    }
}

package com.app.support.admin.views;

import com.app.logistics.inventory.InventoryReservationService;
import com.app.logistics.inventory.entities.Inventory;
import com.app.logistics.inventory.repositories.InventoryRepository;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

/**
 * Vaadin View for Inventory Management and Stock Adjustments
 */
@Route(value = "admin/inventory-native", layout = AdminMainLayout.class)
@PageTitle("Inventory Management | Vasu Admin")
@RolesAllowed({"ADMIN", "OPERATOR"})
public class AdminInventoryView extends VerticalLayout {

    private final InventoryRepository inventoryRepository;
    private final InventoryReservationService inventoryReservationService;
    private final Grid<Inventory> grid = new Grid<>(Inventory.class, false);

    public AdminInventoryView(InventoryRepository inventoryRepository, InventoryReservationService inventoryReservationService) {
        this.inventoryRepository = inventoryRepository;
        this.inventoryReservationService = inventoryReservationService;
        setSizeFull();
        setPadding(true);

        configureGrid();
        Button historyBtn = new Button("View History", VaadinIcon.RECORDS.create(), e -> {
            getUI().ifPresent(ui -> ui.navigate(AdminInventoryTransactionView.class));
        });
        historyBtn.addThemeVariants(ButtonVariant.LUMO_SMALL);
        
        HorizontalLayout header = new HorizontalLayout(new H1("Stock Levels"), historyBtn);
        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.BASELINE);
        
        add(header, grid); // 'filter' component was not defined in the original code, so it's omitted for syntactic correctness.
        updateList();
    }

    private void configureGrid() {
        grid.setSizeFull();
        grid.addColumn(Inventory::getItemCode).setHeader("Item Code").setSortable(true).setFlexGrow(2);
        grid.addColumn(Inventory::getQuantity).setHeader("In Stock").setSortable(true);
        grid.addColumn(Inventory::getReservedQuantity).setHeader("Reserved");
        grid.addColumn(Inventory::getWarehouseId).setHeader("Warehouse");

        grid.addComponentColumn(inv -> {
            Button adjustBtn = new Button("Adjust Stock", VaadinIcon.EDIT.create(), e -> openAdjustmentDialog(inv));
            adjustBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_PRIMARY);
            return adjustBtn;
        }).setHeader("Actions");

        grid.getColumns().forEach(col -> col.setAutoWidth(true));
    }

    private void openAdjustmentDialog(Inventory inv) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Adjust Stock for " + inv.getItemCode());

        IntegerField qtyField = new IntegerField("New Quantity");
        qtyField.setValue(inv.getQuantity());
        qtyField.setWidthFull();
        
        com.vaadin.flow.component.textfield.TextField batchField = new com.vaadin.flow.component.textfield.TextField("Batch / Lot Number (Optional)");
        batchField.setWidthFull();
        
        com.vaadin.flow.component.textfield.TextField reasonField = new com.vaadin.flow.component.textfield.TextField("Reason / Notes");
        reasonField.setValue("Admin Manual Adjustment");
        reasonField.setWidthFull();

        Button saveBtn = new Button("Update", e -> {
            try {
                // Use new adjustStock method which supports batch/expiry logging
                inventoryReservationService.adjustStock(
                    inv.getItemCode(), 
                    qtyField.getValue(), 
                    batchField.getValue(), 
                    null, // Expiry not relevant for fashion
                    reasonField.getValue()
                );
                updateList();
                dialog.close();
                com.vaadin.flow.component.notification.Notification.show("Stock updated for " + inv.getItemCode());
            } catch (Exception ex) {
                com.vaadin.flow.component.notification.Notification.show("Error: " + ex.getMessage());
            }
        });
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancelBtn = new Button("Cancel", e -> dialog.close());

        dialog.getFooter().add(cancelBtn, saveBtn);
        VerticalLayout dialogLayout = new VerticalLayout(qtyField, batchField, reasonField);
        dialog.add(dialogLayout);
        dialog.open();
    }

    private void updateList() {
        try {
            grid.setItems(inventoryRepository.findAll());
        } catch (Exception e) {
            com.vaadin.flow.component.notification.Notification.show("Error loading inventory: " + e.getMessage());
        }
    }
}

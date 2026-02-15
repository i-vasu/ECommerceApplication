package com.app.support.admin.views;

import com.app.logistics.domain.ProcurementService;
import com.app.logistics.entities.PurchaseOrder;
import com.app.logistics.repositories.PurchaseOrderRepo;
import com.app.finance.services.VendorService;
import com.app.finance.payloads.VendorDTO;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import java.util.ArrayList;
import java.util.List;

@Route(value = "admin/procurement", layout = AdminMainLayout.class)
@PageTitle("Procurement & Supply Chain | Vasu Admin")
@RolesAllowed({"ADMIN", "OPERATOR"})
public class AdminProcurementView extends VerticalLayout {

    private final ProcurementService procurementService;
    private final PurchaseOrderRepo poRepo;
    private final VendorService vendorService;
    private final Grid<PurchaseOrder> poGrid = new Grid<>(PurchaseOrder.class, false);

    public AdminProcurementView(ProcurementService procurementService, PurchaseOrderRepo poRepo, VendorService vendorService) {
        this.procurementService = procurementService;
        this.poRepo = poRepo;
        this.vendorService = vendorService;
        
        setSizeFull();
        setSpacing(true);
        setPadding(true);

        add(new H1("Procurement Command Center"));
        
        configureGrid();
        add(new H2("Purchase Orders"), poGrid);
        
        Button createBtn = new Button("Create New PO", e -> openPOCreationDialog());
        createBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        add(createBtn);

        refreshGrid();
    }

    private void configureGrid() {
        poGrid.addColumn(PurchaseOrder::getId).setHeader("PO #").setWidth("80px");
        poGrid.addColumn(PurchaseOrder::getSupplierName).setHeader("Supplier").setFlexGrow(1);
        poGrid.addColumn(PurchaseOrder::getStatus).setHeader("Status").setWidth("120px");
        poGrid.addColumn(PurchaseOrder::getTotalAmount).setHeader("Amount").setWidth("120px");
        poGrid.addColumn(PurchaseOrder::getCreatedAt).setHeader("Created At").setWidth("180px");

        poGrid.addComponentColumn(po -> {
            Button receiveBtn = new Button("Receive Goods", e -> {
                try {
                    procurementService.receiveGoods(po.getId());
                    Notification.show("Goods received and stock updated!");
                    refreshGrid();
                } catch (Exception ex) {
                    Notification.show("Error: " + ex.getMessage());
                }
            });
            receiveBtn.addThemeVariants(ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_SMALL);
            receiveBtn.setEnabled(po.getStatus() != PurchaseOrder.POStatus.RECEIVED);
            return receiveBtn;
        }).setHeader("Actions");
        
        poGrid.setSizeFull();
    }

    private void refreshGrid() {
        poGrid.setItems(poRepo.findAll());
    }

    private void openPOCreationDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("New Purchase Order");
        
        VerticalLayout layout = new VerticalLayout();
        
        ComboBox<VendorDTO> vendorSelect = new ComboBox<>("Select Vendor");
        vendorSelect.setItems(vendorService.getAllVendors());
        vendorSelect.setItemLabelGenerator(VendorDTO::name);
        vendorSelect.setWidthFull();
        
        TextField itemCode = new TextField("Item Code (Primary)");
        TextField quantity = new TextField("Quantity");
        TextField unitPrice = new TextField("Unit Price");
        
        layout.add(vendorSelect, itemCode, quantity, unitPrice);
        
        Button saveBtn = new Button("Create PO", e -> {
            if (vendorSelect.getValue() == null) return;
            
            try {
                // Simplified PO Creation for demo
                com.app.logistics.entities.PurchaseOrderItem item = new com.app.logistics.entities.PurchaseOrderItem();
                item.setItemCode(itemCode.getValue());
                item.setQuantity(Integer.parseInt(quantity.getValue()));
                item.setUnitPrice(Double.parseDouble(unitPrice.getValue()));
                
                procurementService.createPO(vendorSelect.getValue().id(), List.of(item));
                Notification.show("Purchase Order Created!");
                refreshGrid();
                dialog.close();
            } catch (Exception ex) {
                Notification.show("Error: " + ex.getMessage());
            }
        });
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        
        dialog.add(layout);
        dialog.getFooter().add(new Button("Cancel", e -> dialog.close()), saveBtn);
        dialog.open();
    }
}

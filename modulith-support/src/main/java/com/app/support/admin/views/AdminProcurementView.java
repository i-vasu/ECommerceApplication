package com.app.support.admin.views;

import com.app.logistics.domain.ProcurementService;
import com.app.logistics.entities.PurchaseOrder;
import com.app.logistics.repositories.PurchaseOrderRepo;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
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

@Route(value = "admin/procurement", layout = AdminMainLayout.class)
@PageTitle("Procurement & Supply Chain | Vasu Admin")
@RolesAllowed("ADMIN")
public class AdminProcurementView extends VerticalLayout {

    private final ProcurementService procurementService;
    private final PurchaseOrderRepo poRepo;
    private final Grid<PurchaseOrder> poGrid = new Grid<>(PurchaseOrder.class, false);

    public AdminProcurementView(ProcurementService procurementService, PurchaseOrderRepo poRepo) {
        this.procurementService = procurementService;
        this.poRepo = poRepo;
        
        setSizeFull();
        setSpacing(true);
        setPadding(true);

        add(new H1("Procurement Command Center"));
        
        configureGrid();
        add(new H2("Purchase Orders"), poGrid);
        
        Button createBtn = new Button("Create New PO", e -> Notification.show("PO Creation Form pending..."));
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
}

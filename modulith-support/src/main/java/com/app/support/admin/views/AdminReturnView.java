package com.app.support.admin.views;

import com.app.support.domain.ReturnService;
import com.app.support.entities.ReturnRequest;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

@Route(value = "admin/returns", layout = AdminMainLayout.class)
@PageTitle("Returns & RMA | Vasu Admin")
@RolesAllowed("ADMIN")
public class AdminReturnView extends VerticalLayout {

    private final ReturnService returnService;
    private final Grid<ReturnRequest> grid = new Grid<>(ReturnRequest.class, false);

    public AdminReturnView(ReturnService returnService) {
        this.returnService = returnService;
        setSizeFull();
        setPadding(true);

        add(new H1("Returns Management (RMA)"));
        
        configureGrid();
        add(grid);
        refreshGrid();
    }

    private void configureGrid() {
        grid.addColumn(ReturnRequest::getReturnRequestId).setHeader("ID").setWidth("80px");
        grid.addColumn(ReturnRequest::getOrderId).setHeader("Order #");
        grid.addColumn(ReturnRequest::getUserEmail).setHeader("Customer");
        grid.addColumn(ReturnRequest::getStatus).setHeader("Status");
        grid.addColumn(ReturnRequest::getRefundAmount).setHeader("Refund");

        grid.addComponentColumn(req -> {
            Button approveBtn = new Button("Approve & QC", e -> {
                try {
                    returnService.approveReturn(req.getReturnRequestId());
                    Notification.show("Return approved and completed");
                    refreshGrid();
                } catch (Exception ex) {
                    Notification.show("Error: " + ex.getMessage());
                }
            });
            approveBtn.addThemeVariants(ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_SMALL);
            approveBtn.setEnabled(req.getStatus() == ReturnRequest.ReturnStatus.REQUESTED);
            return approveBtn;
        }).setHeader("Actions");

        grid.setSizeFull();
    }

    private void refreshGrid() {
        // This is a placeholder since we don't have a global search yet
        // In a real app we'd fetch all returns
        grid.setItems(returnService.getUserReturns("admin@vasu.com")); // Placeholder logic
    }
}

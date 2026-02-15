package com.app.support.admin.views;

import com.app.support.domain.ReturnService;
import com.app.support.entities.ReturnRequest;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
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
        grid.addColumn(ReturnRequest::getOrderId).setHeader("Order #").setSortable(true);
        grid.addColumn(ReturnRequest::getUserEmail).setHeader("Customer").setSortable(true);
        grid.addColumn(ReturnRequest::getReason).setHeader("Reason");
        grid.addColumn(ReturnRequest::getStatus).setHeader("Status");
        grid.addColumn(req -> req.getCreatedAt().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")))
            .setHeader("Requested On").setSortable(true);
        grid.addColumn(p -> "₹" + p.getRefundAmount()).setHeader("Refund Amount");

        grid.addComponentColumn(req -> {
            HorizontalLayout actions = new HorizontalLayout();
            
            if (req.getStatus() == ReturnRequest.ReturnStatus.REQUESTED) {
                Button approveBtn = new Button("Approve", e -> {
                    try {
                        returnService.approveReturn(req.getReturnRequestId());
                        Notification.show("Return approved. Reverse pickup scheduled.");
                        refreshGrid();
                    } catch (Exception ex) {
                        Notification.show("Error: " + ex.getMessage());
                    }
                });
                approveBtn.addThemeVariants(ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_SMALL);
                
                Button rejectBtn = new Button("Reject", e -> {
                    com.vaadin.flow.component.dialog.Dialog rejectDialog = new com.vaadin.flow.component.dialog.Dialog();
                    rejectDialog.setHeaderTitle("Reject Return #" + req.getReturnRequestId());
                    com.vaadin.flow.component.textfield.TextField reasonField = new com.vaadin.flow.component.textfield.TextField("Reason for Rejection");
                    reasonField.setWidthFull();
                    
                    Button confirmReject = new Button("Reject", ev -> {
                        try {
                            returnService.rejectReturn(req.getReturnRequestId(), reasonField.getValue());
                            Notification.show("Return rejected");
                            rejectDialog.close();
                            refreshGrid();
                        } catch (Exception ex) {
                            Notification.show("Error: " + ex.getMessage());
                        }
                    });
                    confirmReject.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_PRIMARY);
                    
                    rejectDialog.add(reasonField);
                    rejectDialog.getFooter().add(new Button("Cancel", ev -> rejectDialog.close()), confirmReject);
                    rejectDialog.open();
                });
                rejectBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL);
                
                actions.add(approveBtn, rejectBtn);
            } else if (req.getStatus() == ReturnRequest.ReturnStatus.APPROVED) {
                Button receivedBtn = new Button("Item Received", e -> {
                    try {
                        returnService.markAsReceived(req.getReturnRequestId(), "Admin QC passed");
                        Notification.show("Return completed. Refund & Restock triggered.");
                        refreshGrid();
                    } catch (Exception ex) {
                        Notification.show("Error: " + ex.getMessage());
                    }
                });
                receivedBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
                actions.add(receivedBtn);
            }

            return actions;
        }).setHeader("Actions").setWidth("200px");

        grid.setSizeFull();
    }

    private void refreshGrid() {
        grid.setItems(returnService.getAllReturns(0, 100).getContent());
    }
}

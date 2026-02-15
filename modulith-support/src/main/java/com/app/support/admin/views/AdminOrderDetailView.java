package com.app.support.admin.views;

import com.app.order.order.OrderService;
import com.app.order.payloads.OrderDTO;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

@Route(value = "admin/orders/detail", layout = AdminMainLayout.class)
@PageTitle("Order Details | Admin")
@RolesAllowed({"ADMIN", "OPERATOR", "SUPPORT"})
public class AdminOrderDetailView extends VerticalLayout implements HasUrlParameter<Long> {

    private final OrderService orderService;
    private final com.app.logistics.shipping.ShipmentService shipmentService;
    private final com.app.finance.payment.PaymentService paymentService; // Inject PaymentService
    private final VerticalLayout details = new VerticalLayout();
    private final Grid<com.app.order.payloads.OrderItemDTO> itemsGrid = new Grid<>(com.app.order.payloads.OrderItemDTO.class, false);

    public AdminOrderDetailView(OrderService orderService, com.app.logistics.shipping.ShipmentService shipmentService, com.app.finance.payment.PaymentService paymentService) {
        this.orderService = orderService;
        this.shipmentService = shipmentService;
        this.paymentService = paymentService;
        setSpacing(true);
        setPadding(true);

        configureItemsGrid();
        add(new H1("Order Details"), details, new H2("Order Items"), itemsGrid);
    }
    
    // ... existing grid config ...

    private void renderOrder(OrderDTO order) {
        details.removeAll();
        // ... details adding ...
        details.add(new Span("Order ID: #" + order.orderId()));
        details.add(new Span("Customer: " + order.email()));
        details.add(new Span("Date: " + order.orderDate()));
        details.add(new Span("Total: ₹" + order.totalAmount()));
        
        Span status = new Span("Status: " + order.orderStatus());
        status.getElement().getThemeList().add("badge");
        details.add(status);

        HorizontalLayout actions = new HorizontalLayout();
        
        // Cancel Button
        Button cancel = new Button("Cancel Order", e -> {
            orderService.updateOrderStatus(order.orderId(), "CANCELLED");
            Notification.show("Order cancelled");
            setParameter(null, order.orderId());
        });
        cancel.addThemeVariants(ButtonVariant.LUMO_ERROR);
        
        // Ship Button
        Button ship = new Button("Ship Order", e -> {
            try {
                orderService.shipOrder(order.orderId());
                Notification.show("Order shipment initiated!");
                setParameter(null, order.orderId());
            } catch (Exception ex) {
                Notification.show("Error: " + ex.getMessage());
            }
        });
        ship.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        if ("SHIPPED".equals(order.orderStatus()) || "DELIVERED".equals(order.orderStatus())) {
             ship.setEnabled(false);
        }
        
        // Print Label Button
        Button printLabel = new Button("Print Label", e -> {
             try {
                 String labelUrl = shipmentService.getLabelUrl(order.orderId());
                 getUI().ifPresent(ui -> ui.getPage().open(labelUrl, "_blank"));
             } catch (Exception ex) {
                 Notification.show("Error fetching label: " + ex.getMessage());
             }
        });
        printLabel.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
        printLabel.setVisible("SHIPPED".equals(order.orderStatus()) || "DELIVERED".equals(order.orderStatus()));
        
        // Refund Button
        Button refund = new Button("Refund Order", e -> {
            try {
                // Assuming full refund for simplification. 
                // For partial, we'd need a dialog to input amount.
                // We'll use a dialog for confirmation at least.
                com.vaadin.flow.component.dialog.Dialog confirmDialog = new com.vaadin.flow.component.dialog.Dialog();
                confirmDialog.setHeaderTitle("Confirm Refund");
                confirmDialog.add("Are you sure you want to refund this order?");
                
                Button confirmBtn = new Button("Confirm", ev -> {
                    paymentService.processRefundForOrder(order.orderId(), "Admin requested refund");
                    Notification.show("Refund initiated successfully");
                    confirmDialog.close();
                    setParameter(null, order.orderId());
                });
                confirmBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);
                
                Button cancelBtn = new Button("Cancel", ev -> confirmDialog.close());
                
                confirmDialog.getFooter().add(cancelBtn, confirmBtn);
                confirmDialog.open();
                
            } catch (Exception ex) {
                Notification.show("Error processing refund: " + ex.getMessage());
            }
        });
        refund.addThemeVariants(ButtonVariant.LUMO_ERROR);
        // Visible only if paid/shipped/delivered, etc.
        refund.setVisible(!"CANCELLED".equals(order.orderStatus()) && !"PENDING".equals(order.orderStatus())); 

        actions.add(ship, cancel, printLabel, refund);
        details.add(actions);

        itemsGrid.setItems(order.orderItems());
    }

    private void configureItemsGrid() {
        itemsGrid.addColumn(i -> i.product() != null ? i.product().productName() : "Unknown").setHeader("Product");
        itemsGrid.addColumn(com.app.order.payloads.OrderItemDTO::quantity).setHeader("Qty");
        itemsGrid.addColumn(i -> "₹" + i.orderedProductPrice()).setHeader("Price");
        itemsGrid.addColumn(i -> "₹" + (i.orderedProductPrice().doubleValue() * i.quantity())).setHeader("Subtotal");
    }

    @Override
    public void setParameter(BeforeEvent event, Long parameter) {
        if (parameter != null) {
            OrderDTO order = orderService.getOrderById(parameter);
            renderOrder(order);
        }
    }
}

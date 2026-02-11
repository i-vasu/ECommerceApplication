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
@PageTitle("Order Details | Vasu Admin")
@RolesAllowed("ADMIN")
public class AdminOrderDetailView extends VerticalLayout implements HasUrlParameter<Long> {

    private final OrderService orderService;
    private final VerticalLayout details = new VerticalLayout();
    private final Grid<com.app.order.payloads.OrderItemDTO> itemsGrid = new Grid<>(com.app.order.payloads.OrderItemDTO.class, false);

    public AdminOrderDetailView(OrderService orderService) {
        this.orderService = orderService;
        setSpacing(true);
        setPadding(true);

        configureItemsGrid();
        add(new H1("Order Details"), details, new H2("Order Items"), itemsGrid);
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

    private void renderOrder(OrderDTO order) {
        details.removeAll();
        details.add(new Span("Order ID: #" + order.orderId()));
        details.add(new Span("Customer: " + order.email()));
        details.add(new Span("Date: " + order.orderDate()));
        details.add(new Span("Total: ₹" + order.totalAmount()));
        
        Span status = new Span("Status: " + order.orderStatus());
        status.getElement().getThemeList().add("badge");
        details.add(status);

        HorizontalLayout actions = new HorizontalLayout();
        Button cancel = new Button("Cancel Order", e -> {
            orderService.updateOrderStatus(order.orderId(), "CANCELLED");
            Notification.show("Order cancelled");
            setParameter(null, order.orderId());
        });
        cancel.addThemeVariants(ButtonVariant.LUMO_ERROR);
        
        actions.add(cancel);
        details.add(actions);

        itemsGrid.setItems(order.orderItems());
    }
}

package com.app.support.admin.views;

import com.app.order.order.OrderService;
import com.app.order.payloads.OrderDTO;
import com.app.order.payloads.OrderResponse;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

/**
 * Vaadin View for Order Management
 */
@Route(value = "admin/orders-native", layout = AdminMainLayout.class)
@PageTitle("Order Management | Vasu Admin")
@RolesAllowed("ADMIN")
public class AdminOrderListView extends VerticalLayout {

    private final OrderService orderService;
    private final Grid<OrderDTO> grid = new Grid<>(OrderDTO.class, false);

    public AdminOrderListView(OrderService orderService) {
        this.orderService = orderService;
        setSizeFull();
        setPadding(true);

        configureGrid();
        add(new H1("Order Management"), grid);
        updateList();
    }

    private void configureGrid() {
        grid.setSizeFull();
        
        grid.addColumn(OrderDTO::orderId).setHeader("Order ID").setSortable(true).setWidth("100px");
        grid.addColumn(OrderDTO::email).setHeader("Customer").setSortable(true).setFlexGrow(2);
        grid.addColumn(OrderDTO::orderDate).setHeader("Date").setSortable(true).setWidth("180px");
        grid.addColumn(o -> "₹" + o.totalAmount()).setHeader("Total Amount").setSortable(true).setWidth("150px");
        
        grid.addComponentColumn(o -> {
            Span statusBadge = new Span(o.orderStatus());
            statusBadge.getElement().getThemeList().add("badge");
            
            String s = o.orderStatus() != null ? o.orderStatus().toUpperCase() : "";
            String color = "default";
            if (s.contains("DELIVERED") || s.contains("PAID") || s.contains("SUCCESS")) color = "success";
            else if (s.contains("CANCELLED") || s.contains("FAILED")) color = "error";
            else if (s.contains("SHIPPED") || s.contains("PACKED") || s.contains("PICKED")) color = "contrast";
            
            if (!"default".equals(color)) {
                statusBadge.getElement().getThemeList().add(color);
            }
            return statusBadge;
        }).setHeader("Status").setWidth("150px");

        grid.addComponentColumn(o -> {
            Button viewBtn = new Button("View Details", VaadinIcon.EYE.create(), e -> {
                getUI().ifPresent(ui -> ui.navigate(AdminOrderDetailView.class, o.orderId()));
            });
            viewBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
            return viewBtn;
        }).setHeader("Actions").setWidth("150px");

        grid.getColumns().forEach(col -> col.setAutoWidth(true));
    }

    private void updateList() {
        try {
            OrderResponse response = orderService.getAllOrders(0, 100, "orderDate", "desc");
            grid.setItems(response.getContent());
        } catch (Exception e) {
            com.vaadin.flow.component.notification.Notification.show("Error: " + e.getMessage());
        }
    }
}

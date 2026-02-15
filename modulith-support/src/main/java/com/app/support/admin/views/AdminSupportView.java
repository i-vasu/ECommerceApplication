package com.app.support.admin.views;

import com.app.support.domain.SupportService;
import com.app.support.entities.SupportTicket;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

/**
 * Vaadin View for Support Ticket Management
 */
@Route(value = "admin/support-native", layout = AdminMainLayout.class)
@PageTitle("Customer Support | Vasu Admin")
@RolesAllowed({"ADMIN", "SUPPORT"})
public class AdminSupportView extends VerticalLayout {

    private final SupportService supportService;
    private final Grid<SupportTicket> grid = new Grid<>(SupportTicket.class, false);

    public AdminSupportView(SupportService supportService) {
        this.supportService = supportService;
        setSizeFull();
        setPadding(true);

        configureGrid();
        add(new H1("Support Management"), grid);
        updateList();
    }

    private void configureGrid() {
        grid.setSizeFull();
        
        grid.addColumn(SupportTicket::getTicketId).setHeader("ID").setSortable(true).setWidth("100px");
        grid.addColumn(SupportTicket::getUserEmail).setHeader("Customer").setSortable(true).setFlexGrow(1);
        grid.addColumn(SupportTicket::getSubject).setHeader("Subject").setFlexGrow(2);
        
        grid.addComponentColumn(t -> {
            Span status = new Span(t.getStatus());
            status.getElement().getThemeList().add("badge");
            if ("OPEN".equals(t.getStatus())) status.getElement().getThemeList().add("error");
            else if ("IN_PROGRESS".equals(t.getStatus())) status.getElement().getThemeList().add("contrast");
            else status.getElement().getThemeList().add("success");
            return status;
        }).setHeader("Status").setWidth("120px");

        grid.addComponentColumn(t -> {
            Span priority = new Span(t.getPriority());
            priority.getElement().getThemeList().add("badge");
            if ("URGENT".equals(t.getPriority())) priority.getStyle().set("background", "#b91c1c").set("color", "white");
            else if ("HIGH".equals(t.getPriority())) priority.getStyle().set("background", "#c2410c").set("color", "white");
            return priority;
        }).setHeader("Priority").setWidth("120px");

        grid.addComponentColumn(t -> {
            Button replyBtn = new Button(VaadinIcon.CHAT.create(), e -> {
                getUI().ifPresent(ui -> ui.navigate(AdminSupportDetailView.class, t.getTicketId()));
            });
            replyBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
            return replyBtn;
        }).setHeader("Actions").setWidth("120px");

        grid.getColumns().forEach(col -> col.setAutoWidth(true));
    }

    private void updateList() {
        try {
            var tickets = supportService.getAllTickets(PageRequest.of(0, 50, Sort.by("createdAt").descending()));
            grid.setItems(tickets.getContent());
        } catch (Exception e) {
            com.vaadin.flow.component.notification.Notification.show("Error loading tickets: " + e.getMessage());
        }
    }
}

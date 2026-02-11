package com.app.support.admin.views;

import com.app.support.domain.SupportService;
import com.app.support.entities.SupportTicket;
import com.app.support.entities.TicketMessage;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.RolesAllowed;

import java.time.format.DateTimeFormatter;

@Route(value = "admin/support/detail", layout = AdminMainLayout.class)
@PageTitle("Ticket Details | Vasu Admin")
@RolesAllowed("ADMIN")
public class AdminSupportDetailView extends VerticalLayout implements HasUrlParameter<Long> {

    private final SupportService supportService;
    private final VerticalLayout messageHistory = new VerticalLayout();
    private final TextArea replyArea = new TextArea("Send Reply");
    private Long ticketId;

    public AdminSupportDetailView(SupportService supportService) {
        this.supportService = supportService;
        setSpacing(true);
        setPadding(true);

        messageHistory.setWidthFull();
        messageHistory.setSpacing(true);
        
        replyArea.setWidthFull();
        replyArea.setMinHeight("150px");
        replyArea.setPlaceholder("Type your response here...");

        Button sendReply = new Button("Send Reply", e -> sendReply());
        sendReply.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        
        Button closeTicket = new Button("Close Ticket", e -> closeTicket());
        closeTicket.addThemeVariants(ButtonVariant.LUMO_SUCCESS);

        add(new H1("Support Ticket"), messageHistory, replyArea, new HorizontalLayout(sendReply, closeTicket));
    }

    @Override
    public void setParameter(BeforeEvent event, Long parameter) {
        this.ticketId = parameter;
        refreshTicket();
    }

    private void refreshTicket() {
        if (ticketId == null) return;
        
        SupportTicket ticket = supportService.getTicketById(ticketId);
        messageHistory.removeAll();
        
        H3 subjectLine = new H3(ticket.getSubject());
        Span meta = new Span("Customer: " + ticket.getUserEmail() + " | Status: " + ticket.getStatus());
        meta.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.TextColor.SECONDARY);
        messageHistory.add(subjectLine, meta);

        for (TicketMessage msg : ticket.getMessages()) {
            Div bubble = new Div();
            bubble.setWidthFull();
            bubble.getStyle().set("padding", "12px")
                  .set("border-radius", "8px")
                  .set("margin-bottom", "8px");
            
            boolean isAdmin = "ADMIN".equals(msg.getSenderType());
            bubble.getStyle().set("background-color", isAdmin ? "#f0fdf4" : "#f1f5f9")
                  .set("border-left", isAdmin ? "4px solid #22c55e" : "4px solid #64748b");

            Span sender = new Span(isAdmin ? "You (Admin)" : "Customer");
            sender.addClassNames(LumoUtility.FontWeight.BOLD, LumoUtility.FontSize.SMALL);
            
            Span time = new Span(msg.getTimestamp().format(DateTimeFormatter.ofPattern("MMM dd, HH:mm")));
            time.addClassNames(LumoUtility.FontSize.XSMALL, LumoUtility.TextColor.TERTIARY, LumoUtility.Margin.Left.SMALL);
            
            Div text = new Div(new Span(msg.getMessage()));
            text.getStyle().set("margin-top", "4px");

            bubble.add(new HorizontalLayout(sender, time), text);
            messageHistory.add(bubble);
        }
    }

    private void sendReply() {
        String msg = replyArea.getValue();
        if (msg == null || msg.trim().isEmpty()) return;
        
        try {
            supportService.adminReplyToTicket(ticketId, msg, "admin@vasu.com");
            replyArea.clear();
            refreshTicket();
            Notification.show("Reply sent");
        } catch (Exception e) {
            Notification.show("Error: " + e.getMessage());
        }
    }

    private void closeTicket() {
        try {
            supportService.updateTicketStatus(ticketId, "CLOSED");
            Notification.show("Ticket closed");
            getUI().ifPresent(ui -> ui.navigate(AdminSupportView.class));
        } catch (Exception e) {
            Notification.show("Error: " + e.getMessage());
        }
    }
}

package com.app.order.entites;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "support_tickets")
public class SupportTicket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long ticketId;

    private String userEmail;

    private String subject;

    // OPEN, IN_PROGRESS, CLOSED
    private String status = "OPEN";

    private LocalDateTime createdAt = LocalDateTime.now();

    // Optional: Link to an Order
    private Long relatedOrderId;

    @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL)
    private List<TicketMessage> messages = new ArrayList<>();

    public SupportTicket() {
    }

    public SupportTicket(Long ticketId, String userEmail, String subject, String status, LocalDateTime createdAt,
            Long relatedOrderId, List<TicketMessage> messages) {
        this.ticketId = ticketId;
        this.userEmail = userEmail;
        this.subject = subject;
        this.status = status;
        this.createdAt = createdAt;
        this.relatedOrderId = relatedOrderId;
        this.messages = messages;
    }

    public Long getTicketId() {
        return ticketId;
    }

    public void setTicketId(Long ticketId) {
        this.ticketId = ticketId;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Long getRelatedOrderId() {
        return relatedOrderId;
    }

    public void setRelatedOrderId(Long relatedOrderId) {
        this.relatedOrderId = relatedOrderId;
    }

    public List<TicketMessage> getMessages() {
        return messages;
    }

    public void setMessages(List<TicketMessage> messages) {
        this.messages = messages;
    }
}

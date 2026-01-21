package com.app.order.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "ticket_messages")
public class TicketMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long messageId;

    @ManyToOne
    @JoinColumn(name = "ticket_id")
    @JsonIgnore
    private SupportTicket ticket;

    // "USER" or "ADMIN"
    private String senderType;

    // Specifically who sent it (email or admin ID)
    private String senderId;

    @Column(length = 2000)
    private String message;

    private LocalDateTime timestamp = LocalDateTime.now();

    public TicketMessage() {
    }

    public TicketMessage(Long messageId, SupportTicket ticket, String senderType, String senderId, String message,
            LocalDateTime timestamp) {
        this.messageId = messageId;
        this.ticket = ticket;
        this.senderType = senderType;
        this.senderId = senderId;
        this.message = message;
        this.timestamp = timestamp;
    }

    public Long getMessageId() {
        return messageId;
    }

    public void setMessageId(Long messageId) {
        this.messageId = messageId;
    }

    public SupportTicket getTicket() {
        return ticket;
    }

    public void setTicket(SupportTicket ticket) {
        this.ticket = ticket;
    }

    public String getSenderType() {
        return senderType;
    }

    public void setSenderType(String senderType) {
        this.senderType = senderType;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}

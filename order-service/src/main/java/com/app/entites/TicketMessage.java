package com.app.entites;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "ticket_messages")
@Data
@NoArgsConstructor
@AllArgsConstructor
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
}

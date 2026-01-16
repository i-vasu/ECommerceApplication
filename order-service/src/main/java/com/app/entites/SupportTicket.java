package com.app.entites;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "support_tickets")
@Data
@NoArgsConstructor
@AllArgsConstructor
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
    private java.util.List<TicketMessage> messages = new java.util.ArrayList<>();
}

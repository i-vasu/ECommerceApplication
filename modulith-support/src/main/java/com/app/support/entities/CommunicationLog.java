package com.app.support.entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "communication_logs")
@Data
@NoArgsConstructor
public class CommunicationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "recipient_email", nullable = false)
    private String recipientEmail;

    @Column(name = "subject", nullable = false)
    private String subject;

    @Column(name = "body", columnDefinition = "TEXT")
    private String body;

    @Column(name = "type")
    private String type; // e.g. "CUSTOM", "SYSTEM", "MARKETING"

    @Column(name = "status")
    private String status; // e.g. "SENT", "FAILED"

    @Column(name = "sender_id")
    private String senderId; // ID of the admin who sent it, if any

    @CreationTimestamp
    private LocalDateTime createdAt;
}

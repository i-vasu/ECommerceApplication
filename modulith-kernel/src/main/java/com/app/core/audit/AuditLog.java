package com.app.core.audit;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
@Data
@NoArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String userEmail;

    private String action;

    @Column(length = 2000)
    private String details;

    private String ipAddress;

    private String tenantId;

    @CreationTimestamp
    private LocalDateTime timestamp;

    public AuditLog(String userEmail, String action, String details, String ipAddress, String tenantId) {
        this.userEmail = userEmail;
        this.action = action;
        this.details = details;
        this.ipAddress = ipAddress;
        this.tenantId = tenantId;
    }
}

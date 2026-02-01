package com.app.marketing.entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "marketing_broadcasts")
@Data
@NoArgsConstructor
public class MarketingBroadcast {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer broadcastId;

    private String name;

    private String segmentName; // 'ALL', 'VIP', 'CHURN_RISK'

    private String templateName;

    private String status = "DRAFT"; // 'DRAFT', 'SENDING', 'SENT'

    private Integer sentCount = 0;

    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime sentAt;
}

package com.app.marketing.entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "marketing_campaign_settings")
@Data
@NoArgsConstructor
public class MarketingCampaignSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long campaignId;

    @Column(unique = true, nullable = false)
    private String campaignName;
    
    private boolean isActive = true;
    
    private String subjectLine;
    private String subjectLineB;
    
    private Integer frequencyCapDays = 3;
    
    private java.math.BigDecimal minCartValue = java.math.BigDecimal.ZERO;
    
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}

package com.app.marketing.entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "campaign_links")
@Data
@NoArgsConstructor
public class CampaignLink {

    @Id
    private UUID linkId = UUID.randomUUID();

    private String originalUrl;
    
    private String campaignName;
    
    private String userEmail;
    
    private LocalDateTime createdAt = LocalDateTime.now();

    public CampaignLink(String originalUrl, String campaignName, String userEmail) {
        this.originalUrl = originalUrl;
        this.campaignName = campaignName;
        this.userEmail = userEmail;
    }
}

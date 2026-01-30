package com.app.marketing.entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "marketing_logs")
@Data
@NoArgsConstructor
public class MarketingLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long logId;

    private String userEmail;
    
    private String campaignName;
    
    private LocalDateTime sentAt = LocalDateTime.now();
    
    private String referenceId;
    
    private String status = "SENT";

    public MarketingLog(String userEmail, String campaignName, String referenceId) {
        this.userEmail = userEmail;
        this.campaignName = campaignName;
        this.referenceId = referenceId;
    }
}

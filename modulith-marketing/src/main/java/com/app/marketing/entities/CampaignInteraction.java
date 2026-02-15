package com.app.marketing.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "campaign_interactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CampaignInteraction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long interactionId;

    @Column(nullable = false)
    private UUID linkId;

    private String campaignName;

    private String userEmail; // Nullable, if user is not logged in

    private LocalDateTime interactionTime = LocalDateTime.now();

    private String ipAddress;

    private String userAgent;
}

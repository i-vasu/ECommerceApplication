package com.app.marketing.entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "marketing_workflows")
@Data
@NoArgsConstructor
public class MarketingWorkflow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer workflowId;

    private String name;

    private String triggerEvent; // e.g., 'USER_REGISTERED', 'CART_ABANDONED'

    private boolean isActive = true;

    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToMany(mappedBy = "workflow", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @OrderBy("sequenceOrder ASC")
    private List<MarketingWorkflowStep> steps;
}

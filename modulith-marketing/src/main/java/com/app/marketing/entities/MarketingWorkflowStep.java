package com.app.marketing.entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;

@Entity
@Table(name = "marketing_workflow_steps")
@Data
@NoArgsConstructor
public class MarketingWorkflowStep {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer stepId;

    @ManyToOne
    @JoinColumn(name = "workflow_id")
    private MarketingWorkflow workflow;

    private Integer sequenceOrder;

    private String stepType; // 'SEND_EMAIL', 'SEND_WHATSAPP', 'WAIT', 'CONDITION'

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> config; // e.g., {"template": "welcome", "delay_hours": 24}
}

package com.app.marketing.entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "workflow_executions")
@Data
@NoArgsConstructor
public class WorkflowExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long executionId;

    @ManyToOne
    @JoinColumn(name = "workflow_id")
    private MarketingWorkflow workflow;

    private String userEmail;

    private Integer currentStepOrder;

    private LocalDateTime nextRunAt;

    private String status = "IN_PROGRESS"; // 'IN_PROGRESS', 'COMPLETED', 'FAILED'

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    public WorkflowExecution(MarketingWorkflow workflow, String userEmail, Integer startStep, LocalDateTime runAt) {
        this.workflow = workflow;
        this.userEmail = userEmail;
        this.currentStepOrder = startStep;
        this.nextRunAt = runAt;
        this.status = "IN_PROGRESS";
    }
}

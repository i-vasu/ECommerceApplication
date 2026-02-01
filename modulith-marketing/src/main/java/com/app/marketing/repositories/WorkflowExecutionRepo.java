package com.app.marketing.repositories;

import com.app.marketing.entities.WorkflowExecution;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface WorkflowExecutionRepo extends JpaRepository<WorkflowExecution, Long> {
    List<WorkflowExecution> findByStatusAndNextRunAtBefore(String status, LocalDateTime time);
    boolean existsByWorkflowWorkflowIdAndUserEmailAndStatus(Integer workflowId, String userEmail, String status);
    long countByStatusAndNextRunAtBefore(String status, LocalDateTime time);
}

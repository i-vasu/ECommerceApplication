package com.app.marketing.services;

import com.app.marketing.entities.MarketingWorkflow;
import com.app.marketing.entities.MarketingWorkflowStep;
import com.app.marketing.entities.WorkflowExecution;
import com.app.marketing.repositories.MarketingWorkflowRepo;
import com.app.marketing.repositories.WorkflowExecutionRepo;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.observation.annotation.Observed;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Log4j2
@RequiredArgsConstructor
public class WorkflowEngine {

    private final MarketingWorkflowRepo workflowRepo;
    private final WorkflowExecutionRepo executionRepo;
    private final MarketingService marketingService;
    private final RecommendationService recommendationService;
    private final MeterRegistry meterRegistry;
    private final com.app.governance.rules.RuleEngineService ruleEngine;

    @PostConstruct
    public void init() {
        Gauge.builder("marketing.workflow.pending", executionRepo,
                repo -> repo.countByStatusAndNextRunAtBefore("IN_PROGRESS", LocalDateTime.now()))
                .description("Number of marketing workflow steps waiting for execution")
                .register(meterRegistry);
    }

    /**
     * Listener for Platform events.
     * Bridges business reality with marketing automation.
     */
    @org.springframework.context.event.EventListener
    public void onStateTransition(com.app.core.events.StateTransitionEvent event) {
        log.debug("Workflow Engine received event: {} for {} ID {}", event.getEvent(), event.getEntityType(),
                event.getEntityId());

        // Trigger journeys based on high-level transitions
        String triggerKey = event.getEntityType().toUpperCase() + "_" + event.getEvent().toString().toUpperCase();

        // E.g. CART_ABANDON, ORDER_PLACE, RETURN_REQUEST
        triggerWorkflows(triggerKey, "user@example.com", Map.of(
                "entityId", event.getEntityId(),
                "sourceState", event.getSourceState(),
                "targetState", event.getTargetState()));
    }

    /**
     * Trigger workflows based on incoming events.
     */
    @Observed(name = "marketing.workflow.trigger", contextualName = "trigger-customer-journey")
    public void triggerWorkflows(String eventName, String email, Map<String, Object> initialMetadata) {
        List<MarketingWorkflow> activeWorkflows = workflowRepo.findByTriggerEventAndIsActiveTrue(eventName);
        for (MarketingWorkflow wf : activeWorkflows) {
            // Avoid duplicate active executions for the same workflow/user
            if (!executionRepo.existsByWorkflowWorkflowIdAndUserEmailAndStatus(wf.getWorkflowId(), email,
                    "IN_PROGRESS")) {
                log.info("Starting workflow '{}' for user {}", wf.getName(), email);
                WorkflowExecution exec = new WorkflowExecution(wf, email, 1, LocalDateTime.now());
                exec.setMetadata(initialMetadata);
                executionRepo.save(exec);
            }
        }
    }

    /**
     * Background processor for journey steps.
     * Processes 'WAIT', 'SEND_EMAIL', etc.
     */
    @Scheduled(fixedDelay = 60000) // Run every minute
    @Transactional
    @Observed(name = "marketing.workflow.process", contextualName = "process-scheduled-steps")
    public void processWorkflowSteps() {
        List<WorkflowExecution> pendingExecutions = executionRepo.findByStatusAndNextRunAtBefore("IN_PROGRESS",
                LocalDateTime.now());

        for (WorkflowExecution exec : pendingExecutions) {
            processStep(exec);
        }
    }

    private void processStep(WorkflowExecution exec) {
        MarketingWorkflow wf = exec.getWorkflow();
        Optional<MarketingWorkflowStep> stepOpt = wf.getSteps().stream()
                .filter(s -> s.getSequenceOrder().equals(exec.getCurrentStepOrder()))
                .findFirst();

        if (stepOpt.isEmpty()) {
            exec.setStatus("COMPLETED");
            executionRepo.save(exec);
            return;
        }

        MarketingWorkflowStep step = stepOpt.get();
        log.info("Executing step {} type {} for user {}", step.getSequenceOrder(), step.getStepType(),
                exec.getUserEmail());

        boolean moveNext = true;

        switch (step.getStepType()) {
            case "SEND_EMAIL":
                sendEmailStep(exec, step);
                break;
            case "WAIT":
                Integer hours = (Integer) step.getConfig().getOrDefault("delay_hours", 24);
                exec.setNextRunAt(LocalDateTime.now().plusHours(hours));
                moveNext = false; // Stay on the same step but set future nextRunAt
                exec.setCurrentStepOrder(exec.getCurrentStepOrder() + 1); // Point to NEXT step for when we wake up
                break;
            case "CONDITION":
                // Basic condition logic: e.g. "not_purchased"
                moveNext = evaluateCondition(exec, step);
                break;
            default:
                log.warn("Unknown step type: {}", step.getStepType());
        }

        if (moveNext) {
            exec.setCurrentStepOrder(exec.getCurrentStepOrder() + 1);
            exec.setNextRunAt(LocalDateTime.now());
            // If it was the last step, the next run will find no step and mark COMPLETED
        }

        executionRepo.save(exec);
    }

    private void sendEmailStep(WorkflowExecution exec, MarketingWorkflowStep step) {
        String template = (String) step.getConfig().get("template");
        String subject = (String) step.getConfig().get("subject");

        Map<String, Object> vars = new java.util.HashMap<>();
        vars.put("email", exec.getUserEmail());
        vars.put("recommendations", recommendationService.getTrendingProducts());

        marketingService.sendCampaignEmail(
                exec.getWorkflow().getName(),
                exec.getUserEmail(),
                subject,
                "emails/" + template,
                vars);
    }

    private boolean evaluateCondition(WorkflowExecution exec, MarketingWorkflowStep step) {
        String expression = (String) step.getConfig().get("condition_expression");
        if (expression == null || expression.isBlank()) {
            return true;
        }

        Map<String, Object> context = new java.util.HashMap<>(exec.getMetadata());
        context.put("email", exec.getUserEmail());

        return ruleEngine.evaluate(expression, context);
    }
}

package com.app.marketing.repositories;

import com.app.marketing.entities.MarketingWorkflow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MarketingWorkflowRepo extends JpaRepository<MarketingWorkflow, Integer> {
    List<MarketingWorkflow> findByTriggerEventAndIsActiveTrue(String triggerEvent);
}

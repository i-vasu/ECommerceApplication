package com.app.marketing.repositories;

import com.app.marketing.entities.MarketingLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface MarketingLogRepo extends JpaRepository<MarketingLog, Long> {
    
    Optional<MarketingLog> findByUserEmailAndCampaignNameAndReferenceId(
            String userEmail, String campaignName, String referenceId);

    boolean existsByUserEmailAndCampaignNameAndSentAtAfter(
            String userEmail, String campaignName, LocalDateTime after);
}

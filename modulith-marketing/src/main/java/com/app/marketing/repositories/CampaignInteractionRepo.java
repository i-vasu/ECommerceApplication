package com.app.marketing.repositories;

import com.app.marketing.entities.CampaignInteraction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CampaignInteractionRepo extends JpaRepository<CampaignInteraction, Long> {
    long countByLinkId(UUID linkId);
}

package com.app.marketing.services;

import com.app.marketing.entities.CampaignInteraction;
import com.app.marketing.entities.CampaignLink;
import com.app.marketing.repositories.CampaignInteractionRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Log4j2
public class CampaignInteractionService {

    private final CampaignInteractionRepo interactionRepo;

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordInteraction(CampaignLink link, String ipAddress, String userAgent) {
        try {
            CampaignInteraction interaction = new CampaignInteraction();
            interaction.setLinkId(link.getLinkId());
            interaction.setCampaignName(link.getCampaignName());
            interaction.setUserEmail(link.getUserEmail());
            interaction.setIpAddress(ipAddress);
            interaction.setUserAgent(userAgent);
            
            interactionRepo.save(interaction);
            log.debug("Recorded interaction for link: {}", link.getLinkId());
        } catch (Exception e) {
            log.error("Failed to record campaign interaction for link {}: {}", link.getLinkId(), e.getMessage());
        }
    }
}

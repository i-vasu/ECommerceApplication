package com.app.marketing.controllers;

import com.app.core.events.UserUnsubscribedEvent;
import com.app.marketing.repositories.CampaignLinkRepo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.view.RedirectView;

import java.util.UUID;

@Log4j2
@Controller
@RequestMapping("/api/mkt")
@RequiredArgsConstructor
public class MarketingController {

    private final CampaignLinkRepo linkRepo;
    private final ApplicationEventPublisher eventPublisher;
    private final com.app.marketing.services.CampaignInteractionService interactionService;

    @GetMapping("/c/{linkId}")
    public RedirectView trackClick(@PathVariable UUID linkId, HttpServletRequest request) {
        return linkRepo.findById(linkId).map(link -> {
            log.info("Ad-Click tracked for campaign: {} by {}", link.getCampaignName(), link.getUserEmail());

            // Fixed: Record interaction via dedicated service
            String ipAddress = request.getRemoteAddr();
            String userAgent = request.getHeader("User-Agent");
            interactionService.recordInteraction(link, ipAddress, userAgent);

            RedirectView redirectView = new RedirectView(link.getOriginalUrl());
            return redirectView;
        }).orElse(new RedirectView("http://localhost:3000"));
    }

    @GetMapping("/unsubscribe/{email}")
    public String unsubscribe(@PathVariable String email) {
        // Publish event — security module handles the user update
        eventPublisher.publishEvent(new UserUnsubscribedEvent(email));
        log.info("User {} requested unsubscribe", email);
        return "marketing/unsubscribed";
    }
}


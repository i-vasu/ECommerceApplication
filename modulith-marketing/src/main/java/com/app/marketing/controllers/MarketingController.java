package com.app.marketing.controllers;

import com.app.marketing.repositories.CampaignLinkRepo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.jdbc.core.JdbcTemplate;
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
    private final JdbcTemplate jdbcTemplate;

    @GetMapping("/c/{linkId}")
    public RedirectView trackClick(@PathVariable UUID linkId, HttpServletRequest request) {
        return linkRepo.findById(linkId).map(link -> {
            log.info("Ad-Click tracked for campaign: {} by {}", link.getCampaignName(), link.getUserEmail());
            
            // Record interaction
            jdbcTemplate.update(
                "INSERT INTO campaign_interactions (link_id, interaction_type, ip_address, user_agent) VALUES (?, ?, ?, ?)",
                linkId, "CLICK", request.getRemoteAddr(), request.getHeader("User-Agent")
            );

            // Return redirect with campaign cookie for session attribution
            RedirectView redirectView = new RedirectView(link.getOriginalUrl());
            // In a real app, we'd set a cookie here for 'attributed_campaign'
            return redirectView;
        }).orElse(new RedirectView("http://localhost:3000"));
    }

    @GetMapping("/unsubscribe/{email}")
    public String unsubscribe(@PathVariable String email) {
        jdbcTemplate.update("UPDATE users SET unsubscribed = TRUE WHERE email = ?", email);
        return "marketing/unsubscribed";
    }
}

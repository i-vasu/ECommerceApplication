package com.app.marketing.listeners;

import com.app.core.events.OrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Log4j2
@Component
@RequiredArgsConstructor
public class TrackingEventListener {

    private final JdbcTemplate jdbcTemplate;

    @EventListener
    public void onOrderCreated(OrderCreatedEvent event) {
        log.info("Attributing order {} for user {}", event.orderId(), event.email());
        
        // Attribution logic: Find the most recent click for this user in the last 48 hours
        String campaign = jdbcTemplate.queryForObject(
            "SELECT cl.campaign_name " +
            "FROM campaign_links cl " +
            "JOIN campaign_interactions ci ON cl.link_id = ci.link_id " +
            "WHERE cl.user_email = ? AND ci.interaction_type = 'CLICK' " +
            "AND ci.created_at > (NOW() - INTERVAL '48 hours') " +
            "ORDER BY ci.created_at DESC LIMIT 1",
            String.class, event.email());

        if (campaign != null) {
            log.info("Order {} attributed to campaign: {}", event.orderId(), campaign);
            jdbcTemplate.update("UPDATE orders SET attributed_campaign = ? WHERE order_id = ?", 
                campaign, event.orderId());
        }
    }
}

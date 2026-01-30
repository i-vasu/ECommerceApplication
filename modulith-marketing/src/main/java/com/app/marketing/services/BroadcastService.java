package com.app.marketing.services;

import com.app.marketing.entities.MarketingBroadcast;
import com.app.marketing.repositories.MarketingBroadcastRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Log4j2
@RequiredArgsConstructor
public class BroadcastService {

    private final MarketingBroadcastRepo broadcastRepo;
    private final MarketingService marketingService;
    private final JdbcTemplate jdbcTemplate;

    @Async
    @Transactional
    public void executeBroadcast(Integer broadcastId) {
        MarketingBroadcast broadcast = broadcastRepo.findById(broadcastId).orElseThrow();
        if ("SENDING".equals(broadcast.getStatus())) return;

        broadcast.setStatus("SENDING");
        broadcastRepo.save(broadcast);

        log.info("Starting broadcast: {}", broadcast.getName());

        String userQuery = "SELECT email, first_name FROM users";
        if ("VIP".equals(broadcast.getSegmentName())) {
            userQuery += " WHERE user_id IN (SELECT user_id FROM orders GROUP BY user_id HAVING SUM(total_amount) > 10000)";
        } else if ("CHURN_RISK".equals(broadcast.getSegmentName())) {
            userQuery += " WHERE user_id NOT IN (SELECT user_id FROM orders WHERE created_at > (NOW() - INTERVAL '60 days'))";
        }

        List<Map<String, Object>> recipients = jdbcTemplate.queryForList(userQuery);
        int count = 0;

        for (Map<String, Object> user : recipients) {
            try {
                Map<String, Object> vars = new HashMap<>();
                vars.put("name", user.get("first_name"));
                
                marketingService.sendCampaignEmail(
                    broadcast.getName(),
                    (String) user.get("email"),
                    "Important Update from Vaabhi",
                    "emails/" + broadcast.getTemplateName(),
                    vars
                );
                count++;
            } catch (Exception e) {
                log.error("Failed to send broadcast to {}: {}", user.get("email"), e.getMessage());
            }
        }

        broadcast.setStatus("SENT");
        broadcast.setSentCount(count);
        broadcast.setSentAt(LocalDateTime.now());
        broadcastRepo.save(broadcast);

        log.info("Broadcast '{}' completed. Sent to {} users.", broadcast.getName(), count);
    }
}

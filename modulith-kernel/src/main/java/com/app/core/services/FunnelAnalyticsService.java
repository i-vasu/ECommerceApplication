package com.app.core.services;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Funnel Analytics Service.
 * Tracks conversion rates across the customer journey.
 */
@Service
@RequiredArgsConstructor
public class FunnelAnalyticsService {

    private final JdbcTemplate jdbcTemplate;

    public Map<String, Object> getConversionFunnel() {
        String sql = "SELECT " +
                "(SELECT COUNT(*) FROM carts) as total_carts, " +
                "(SELECT COUNT(*) FROM orders) as total_orders, " +
                "(SELECT COUNT(*) FROM orders WHERE order_status = 'DELIVERED') as completed_orders";

        Map<String, Object> counts = jdbcTemplate.queryForMap(sql);

        long carts = ((Number) counts.get("total_carts")).longValue();
        long orders = ((Number) counts.get("total_orders")).longValue();
        long delivered = ((Number) counts.get("completed_orders")).longValue();

        Map<String, Object> funnel = new HashMap<>();
        funnel.put("carts", carts);
        funnel.put("orders", orders);
        funnel.put("delivered", delivered);

        funnel.put("cartToOrderRate", carts > 0 ? (double) orders / carts : 0.0);
        funnel.put("fulfillmentRate", orders > 0 ? (double) delivered / orders : 0.0);

        return funnel;
    }
}

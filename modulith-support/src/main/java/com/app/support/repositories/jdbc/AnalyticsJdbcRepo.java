package com.app.support.repositories.jdbc;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.Repository;
import java.util.List;
import java.util.Map;

public interface AnalyticsJdbcRepo extends Repository<Object, Long> {

    @Query("SELECT order_date, gross_revenue FROM view_daily_revenue ORDER BY order_date ASC LIMIT 7")
    List<Map<String, Object>> getDailyRevenue();

    @Query("SELECT * FROM view_funnel_stats")
    List<Map<String, Object>> getFunnelStats();

    @Query("SELECT * FROM view_top_products ORDER BY total_revenue DESC LIMIT 10")
    List<Map<String, Object>> getTopProducts();

    @Query("SELECT * FROM view_user_growth LIMIT 30")
    List<Map<String, Object>> getUserGrowth();

    @Query("SELECT * FROM view_search_performance LIMIT 20")
    List<Map<String, Object>> getSearchPerformance();

    @Query("SELECT * FROM view_inventory_health WHERE status IN ('OUT_OF_STOCK', 'LOW_STOCK') LIMIT 20")
    List<Map<String, Object>> getInventoryHealth();

    @Query("SELECT * FROM view_user_activity ORDER BY lifetime_value DESC LIMIT 10")
    List<Map<String, Object>> getUserActivity();

    @Query("SELECT * FROM view_churn_risk LIMIT 10")
    List<Map<String, Object>> getChurnRisk();

    @Query("SELECT COALESCE(SUM(total_amount), 0) as grand_total, COUNT(*) as total_count FROM orders WHERE order_status != 'CANCELLED'")
    Map<String, Object> getSalesTotals();

    @Query("SELECT COUNT(*) as user_count FROM users")
    Map<String, Object> getUserCount();

    @Query("SELECT * FROM view_daily_revenue ORDER BY order_date DESC LIMIT 30")
    List<Map<String, Object>> getDailyRevenueRecent();
}

package com.app.support.repositories.jdbc;

import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.Repository;
import java.util.List;
import java.util.Map;

public interface MonitoringJdbcRepo extends Repository<Object, Long> {

    @Query("SELECT * FROM monitoring_alert_rules")
    List<Map<String, Object>> getAlertRules();

    @Modifying
    @Query("INSERT INTO monitoring_alert_rules (name, metric_name, threshold, comparison_operator) VALUES (:name, :metric, :threshold, :op)")
    void insertAlertRule(String name, String metric, Double threshold, String op);

    @Query("SELECT * FROM monitoring_alert_history ORDER BY triggered_at DESC LIMIT 50")
    List<Map<String, Object>> getAlertHistory();
}

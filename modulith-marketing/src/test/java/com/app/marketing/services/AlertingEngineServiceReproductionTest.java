package com.app.marketing.services;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.search.Search;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlertingEngineServiceReproductionTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private MeterRegistry meterRegistry;

    @Mock
    private EmailService emailService;

    @Mock
    private WhatsAppGateway whatsappGateway;

    @Mock
    private Search search;

    @InjectMocks
    private AlertingEngineService alertingEngineService;

    @Test
    @DisplayName("BEHAVIOR: Should evaluate rules when table exists")
    void testEvaluateAlertRules_Success() {
        // Arrange
        Map<String, Object> rule = new HashMap<>();
        rule.put("rule_id", 1);
        rule.put("name", "High CPU");
        rule.put("metric_name", "system.cpu.usage");
        rule.put("threshold", 0.9);
        rule.put("comparison_operator", ">");
        rule.put("is_active", true);

        when(jdbcTemplate.queryForList("SELECT * FROM monitoring_alert_rules WHERE is_active = TRUE"))
            .thenReturn(List.of(rule));
        
        when(meterRegistry.find("system.cpu.usage")).thenReturn(search);
        when(search.gauge()).thenReturn(null);
        when(search.meter()).thenReturn(null);

        // Act
        alertingEngineService.evaluateAlertRules();

        // Assert
        verify(jdbcTemplate).queryForList("SELECT * FROM monitoring_alert_rules WHERE is_active = TRUE");
    }

    @Test
    @DisplayName("BEHAVIOR: Should handle missing table gracefully")
    void testEvaluateAlertRules_HandleMissingTable() {
        // Arrange
        when(jdbcTemplate.queryForList("SELECT * FROM monitoring_alert_rules WHERE is_active = TRUE"))
            .thenThrow(new BadSqlGrammarException("StatementCallback", 
                "SELECT * FROM monitoring_alert_rules WHERE is_active = TRUE", 
                new org.postgresql.util.PSQLException("ERROR: relation \"monitoring_alert_rules\" does not exist", null)));

        // Act & Assert (Should not throw exception)
        alertingEngineService.evaluateAlertRules();
        
        verify(jdbcTemplate).queryForList("SELECT * FROM monitoring_alert_rules WHERE is_active = TRUE");
    }
}

package com.app.logistics.shipping.services;

import com.app.logistics.external.ShiprocketClient;
import com.app.logistics.shipping.ShiprocketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * COD (Cash on Delivery) Remittance Service
 * Tracks COD collections and remittances from couriers
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CodRemittanceService {

    private final ShiprocketClient shiprocketClient;
    private final ShiprocketService shiprocketService;

    /**
     * Get COD remittance report
     */
    public Map<String, Object> getCodRemittance(LocalDate fromDate, LocalDate toDate) {
        Map<String, Object> filters = new HashMap<>();
        
        if (fromDate != null) {
            filters.put("from_date", fromDate.toString());
        }
        if (toDate != null) {
            filters.put("to_date", toDate.toString());
        }
        
        try {
            return shiprocketClient.getCodRemittance(shiprocketService.getBearerToken(), filters);
        } catch (Exception e) {
            log.error("Failed to fetch COD remittance: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch COD remittance", e);
        }
    }

    /**
     * Get pending COD collections
     */
    public Map<String, Object> getPendingCodCollections() {
        Map<String, Object> filters = new HashMap<>();
        filters.put("status", "pending");
        
        return getCodRemittance(null, null);
    }

    /**
     * Calculate total COD amount pending
     */
    public Map<String, Object> calculatePendingCodAmount() {
        try {
            Map<String, Object> response = getPendingCodCollections();
            
            double totalPending = 0.0;
            int orderCount = 0;
            
            if (response != null && response.containsKey("data")) {
                List<Map<String, Object>> collections = 
                    (List<Map<String, Object>>) response.get("data");
                
                for (Map<String, Object> collection : collections) {
                    if (collection.containsKey("cod_amount")) {
                        totalPending += ((Number) collection.get("cod_amount")).doubleValue();
                        orderCount++;
                    }
                }
            }
            
            Map<String, Object> summary = new HashMap<>();
            summary.put("total_pending_amount", totalPending);
            summary.put("pending_order_count", orderCount);
            summary.put("average_cod_value", orderCount > 0 ? totalPending / orderCount : 0);
            
            return summary;
            
        } catch (Exception e) {
            log.error("Failed to calculate pending COD: {}", e.getMessage(), e);
            return Map.of("error", e.getMessage());
        }
    }

    /**
     * Get COD remittance summary for a date range
     */
    public Map<String, Object> getCodSummary(LocalDate fromDate, LocalDate toDate) {
        try {
            Map<String, Object> response = getCodRemittance(fromDate, toDate);
            
            double totalCollected = 0.0;
            double totalRemitted = 0.0;
            int totalOrders = 0;
            
            if (response != null && response.containsKey("data")) {
                List<Map<String, Object>> remittances = 
                    (List<Map<String, Object>>) response.get("data");
                
                for (Map<String, Object> remittance : remittances) {
                    if (remittance.containsKey("collected_amount")) {
                        totalCollected += ((Number) remittance.get("collected_amount")).doubleValue();
                    }
                    if (remittance.containsKey("remitted_amount")) {
                        totalRemitted += ((Number) remittance.get("remitted_amount")).doubleValue();
                    }
                    totalOrders++;
                }
            }
            
            Map<String, Object> summary = new HashMap<>();
            summary.put("total_collected", totalCollected);
            summary.put("total_remitted", totalRemitted);
            summary.put("pending_remittance", totalCollected - totalRemitted);
            summary.put("total_orders", totalOrders);
            summary.put("from_date", fromDate);
            summary.put("to_date", toDate);
            
            return summary;
            
        } catch (Exception e) {
            log.error("Failed to generate COD summary: {}", e.getMessage(), e);
            return Map.of("error", e.getMessage());
        }
    }
}

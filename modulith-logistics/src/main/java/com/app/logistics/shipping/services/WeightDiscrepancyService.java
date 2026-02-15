package com.app.logistics.shipping.services;

import com.app.logistics.external.ShiprocketClient;
import com.app.logistics.shipping.ShiprocketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Weight Discrepancy Management Service
 * Monitors and disputes incorrect weight charges from couriers
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class WeightDiscrepancyService {

    private final ShiprocketClient shiprocketClient;
    private final ShiprocketService shiprocketService;

    /**
     * Get all weight discrepancies
     */
    public Map<String, Object> getWeightDiscrepancies(Map<String, Object> filters) {
        try {
            return shiprocketClient.getWeightDiscrepancies(
                shiprocketService.getBearerToken(), filters);
        } catch (Exception e) {
            log.error("Failed to fetch weight discrepancies: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch weight discrepancies", e);
        }
    }

    /**
     * Get weight discrepancies for a specific AWB
     */
    public Map<String, Object> getDiscrepancyByAwb(String awbNumber) {
        Map<String, Object> filters = new HashMap<>();
        filters.put("awb", awbNumber);
        
        return getWeightDiscrepancies(filters);
    }

    /**
     * Raise a weight dispute
     * 
     * @param awbNumber AWB number
     * @param actualWeight Actual weight in kg
     * @param chargedWeight Weight charged by courier in kg
     * @param remarks Reason for dispute
     */
    public Map<String, Object> raiseWeightDispute(String awbNumber, double actualWeight, 
                                                   double chargedWeight, String remarks) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("awb", awbNumber);
        payload.put("actual_weight", actualWeight);
        payload.put("charged_weight", chargedWeight);
        payload.put("remarks", remarks);
        
        try {
            Map<String, Object> response = shiprocketClient.raiseWeightDispute(
                shiprocketService.getBearerToken(), payload);
            
            log.info("Weight dispute raised for AWB {}: Actual={}kg, Charged={}kg", 
                     awbNumber, actualWeight, chargedWeight);
            return response;
            
        } catch (Exception e) {
            log.error("Failed to raise weight dispute for AWB {}: {}", awbNumber, e.getMessage(), e);
            throw new RuntimeException("Failed to raise weight dispute", e);
        }
    }

    /**
     * Auto-detect and raise disputes for significant weight discrepancies
     * Can be scheduled to run daily
     */
    public void autoRaiseDisputes(double thresholdPercentage) {
        try {
            Map<String, Object> response = getWeightDiscrepancies(new HashMap<>());
            
            if (response != null && response.containsKey("data")) {
                List<Map<String, Object>> discrepancies = 
                    (List<Map<String, Object>>) response.get("data");
                
                for (Map<String, Object> disc : discrepancies) {
                    String awb = (String) disc.get("awb");
                    double actualWeight = ((Number) disc.get("actual_weight")).doubleValue();
                    double chargedWeight = ((Number) disc.get("charged_weight")).doubleValue();
                    
                    // Calculate percentage difference
                    double diff = ((chargedWeight - actualWeight) / actualWeight) * 100;
                    
                    // Raise dispute if difference exceeds threshold
                    if (diff > thresholdPercentage) {
                        raiseWeightDispute(awb, actualWeight, chargedWeight, 
                            String.format("Auto-dispute: %.2f%% overcharge detected", diff));
                        
                        log.info("Auto-raised dispute for AWB {}: {}% overcharge", awb, diff);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to auto-raise weight disputes: {}", e.getMessage(), e);
        }
    }

    /**
     * Calculate potential savings from weight disputes
     */
    public Map<String, Object> calculatePotentialSavings() {
        try {
            Map<String, Object> response = getWeightDiscrepancies(new HashMap<>());
            
            double totalSavings = 0.0;
            int disputeCount = 0;
            
            if (response != null && response.containsKey("data")) {
                List<Map<String, Object>> discrepancies = 
                    (List<Map<String, Object>>) response.get("data");
                
                for (Map<String, Object> disc : discrepancies) {
                    if (disc.containsKey("overcharged_amount")) {
                        totalSavings += ((Number) disc.get("overcharged_amount")).doubleValue();
                        disputeCount++;
                    }
                }
            }
            
            Map<String, Object> savings = new HashMap<>();
            savings.put("total_savings", totalSavings);
            savings.put("dispute_count", disputeCount);
            savings.put("average_per_dispute", disputeCount > 0 ? totalSavings / disputeCount : 0);
            
            return savings;
            
        } catch (Exception e) {
            log.error("Failed to calculate savings: {}", e.getMessage(), e);
            return Map.of("error", e.getMessage());
        }
    }
}

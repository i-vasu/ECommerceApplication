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
 * NDR (Non-Delivery Report) Management Service
 * Handles failed delivery attempts and automatic retry logic
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class NdrManagementService {

    private final ShiprocketClient shiprocketClient;
    private final ShiprocketService shiprocketService;

    /**
     * Get all pending NDR shipments
     */
    public Map<String, Object> getPendingNdrs() {
        Map<String, Object> filters = new HashMap<>();
        filters.put("status", "pending");
        
        try {
            return shiprocketClient.getNdrList(shiprocketService.getBearerToken(), filters);
        } catch (Exception e) {
            log.error("Failed to fetch NDR list: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch NDR list", e);
        }
    }

    /**
     * Get NDRs for a specific AWB
     */
    public Map<String, Object> getNdrByAwb(String awbNumber) {
        Map<String, Object> filters = new HashMap<>();
        filters.put("awb", awbNumber);
        
        try {
            return shiprocketClient.getNdrList(shiprocketService.getBearerToken(), filters);
        } catch (Exception e) {
            log.error("Failed to fetch NDR for AWB {}: {}", awbNumber, e.getMessage(), e);
            throw new RuntimeException("Failed to fetch NDR", e);
        }
    }

    /**
     * Take action on NDR - Re-attempt delivery
     * 
     * @param awbNumber AWB number
     * @param action Action to take: "re_attempt", "rto", "change_address"
     * @param remarks Optional remarks
     */
    public Map<String, Object> reAttemptDelivery(String awbNumber, String action, String remarks) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("awb", awbNumber);
        payload.put("action", action);
        
        if (remarks != null && !remarks.isEmpty()) {
            payload.put("remarks", remarks);
        }
        
        try {
            Map<String, Object> response = shiprocketClient.takeNdrAction(
                shiprocketService.getBearerToken(), payload);
            
            log.info("NDR action taken for AWB {}: {}", awbNumber, action);
            return response;
            
        } catch (Exception e) {
            log.error("Failed to take NDR action for AWB {}: {}", awbNumber, e.getMessage(), e);
            throw new RuntimeException("Failed to process NDR action", e);
        }
    }

    /**
     * Auto-retry all pending NDRs (can be scheduled)
     */
    public void autoRetryPendingNdrs() {
        try {
            Map<String, Object> response = getPendingNdrs();
            
            if (response != null && response.containsKey("data")) {
                List<Map<String, Object>> ndrs = (List<Map<String, Object>>) response.get("data");
                
                for (Map<String, Object> ndr : ndrs) {
                    String awb = (String) ndr.get("awb");
                    String ndrStatus = (String) ndr.get("ndr_status");
                    
                    // Auto re-attempt for specific NDR reasons
                    if ("customer_not_available".equals(ndrStatus) || 
                        "address_issue".equals(ndrStatus)) {
                        
                        reAttemptDelivery(awb, "re_attempt", "Auto-retry by system");
                        log.info("Auto-retried NDR for AWB: {}", awb);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to auto-retry NDRs: {}", e.getMessage(), e);
        }
    }
}

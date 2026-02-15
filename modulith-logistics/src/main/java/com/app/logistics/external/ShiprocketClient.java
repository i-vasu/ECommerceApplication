package com.app.logistics.external;

import java.util.Map;

public interface ShiprocketClient {

    Map<String, Object> login(Map<String, String> credentials);

    Map<String, Object> createOrder(String authorization, Map<String, Object> orderPayload);
    
    Map<String, Object> createReverseOrder(String authorization, Map<String, Object> payload);

    Map<String, Object> generateAwb(String authorization, Map<String, Object> payload);

    Map<String, Object> requestPickup(String authorization, Map<String, Object> payload);

    Map<String, Object> trackByAwb(String authorization, String awbNumber);

    Map<String, Object> trackByShipmentId(String authorization, String shipmentId);

    Map<String, Object> getAvailableCouriers(String authorization, Map<String, Object> payload);

    Map<String, Object> cancelOrder(String authorization, Map<String, Object> payload);

    Map<String, Object> getOrder(String authorization, String orderId);

    Map<String, Object> getPickupLocations(String authorization);

    Map<String, Object> generateLabel(String authorization, Map<String, Object> payload);

    Map<String, Object> generateInvoice(String authorization, Map<String, Object> payload);
    
    // NDR (Non-Delivery Report) Management
    Map<String, Object> getNdrList(String authorization, Map<String, Object> filters);
    
    Map<String, Object> takeNdrAction(String authorization, Map<String, Object> payload);
    
    // Weight Discrepancy
    Map<String, Object> getWeightDiscrepancies(String authorization, Map<String, Object> filters);
    
    Map<String, Object> raiseWeightDispute(String authorization, Map<String, Object> payload);
    
    // COD Remittance
    Map<String, Object> getCodRemittance(String authorization, Map<String, Object> filters);
}

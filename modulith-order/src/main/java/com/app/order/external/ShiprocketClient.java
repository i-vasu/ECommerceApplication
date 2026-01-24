package com.app.order.external;

import java.util.Map;

public interface ShiprocketClient {

        Map<String, Object> login(Map<String, String> credentials);

        Map<String, Object> createOrder(String authorization, Map<String, Object> orderPayload);

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
}

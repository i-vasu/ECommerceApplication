package com.app.order.external;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class ShiprocketRestClient implements ShiprocketClient {

    private final RestClient restClient;
    private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE = new ParameterizedTypeReference<>() {
    };

    public ShiprocketRestClient(
            @Value("${shiprocket.api.url:https://apiv2.shiprocket.in/v1/external}") String baseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    @Override
    public Map<String, Object> login(Map<String, String> credentials) {
        return restClient.post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(credentials)
                .retrieve()
                .body(MAP_TYPE);
    }

    @Override
    public Map<String, Object> createOrder(String authorization, Map<String, Object> orderPayload) {
        return restClient.post()
                .uri("/orders/create/adhoc")
                .header("Authorization", authorization)
                .contentType(MediaType.APPLICATION_JSON)
                .body(orderPayload)
                .retrieve()
                .body(MAP_TYPE);
    }

    @Override
    public Map<String, Object> generateAwb(String authorization, Map<String, Object> payload) {
        return restClient.post()
                .uri("/courier/assign/awb")
                .header("Authorization", authorization)
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .body(MAP_TYPE);
    }

    @Override
    public Map<String, Object> requestPickup(String authorization, Map<String, Object> payload) {
        return restClient.post()
                .uri("/courier/generate/pickup")
                .header("Authorization", authorization)
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .body(MAP_TYPE);
    }

    @Override
    public Map<String, Object> trackByAwb(String authorization, String awbNumber) {
        return restClient.get()
                .uri("/courier/track/awb/{awbNumber}", awbNumber)
                .header("Authorization", authorization)
                .retrieve()
                .body(MAP_TYPE);
    }

    @Override
    public Map<String, Object> trackByShipmentId(String authorization, String shipmentId) {
        return restClient.get()
                .uri("/courier/track/shipment/{shipmentId}", shipmentId)
                .header("Authorization", authorization)
                .retrieve()
                .body(MAP_TYPE);
    }

    @Override
    public Map<String, Object> getAvailableCouriers(String authorization, Map<String, Object> payload) {
        // Mapping body map to query params as GET body is not supported/standard
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/courier/serviceability");
        payload.forEach((k, v) -> builder.queryParam(k, v));

        return restClient.get()
                .uri(builder.toUriString())
                .header("Authorization", authorization)
                .retrieve()
                .body(MAP_TYPE);
    }

    @Override
    public Map<String, Object> cancelOrder(String authorization, Map<String, Object> payload) {
        return restClient.post()
                .uri("/orders/cancel")
                .header("Authorization", authorization)
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .body(MAP_TYPE);
    }

    @Override
    public Map<String, Object> getOrder(String authorization, String orderId) {
        return restClient.get()
                .uri("/orders/show/{orderId}", orderId)
                .header("Authorization", authorization)
                .retrieve()
                .body(MAP_TYPE);
    }

    @Override
    public Map<String, Object> getPickupLocations(String authorization) {
        return restClient.get()
                .uri("/settings/company/pickup")
                .header("Authorization", authorization)
                .retrieve()
                .body(MAP_TYPE);
    }

    @Override
    public Map<String, Object> generateLabel(String authorization, Map<String, Object> payload) {
        return restClient.post()
                .uri("/courier/generate/label")
                .header("Authorization", authorization)
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .body(MAP_TYPE);
    }

    @Override
    public Map<String, Object> generateInvoice(String authorization, Map<String, Object> payload) {
        return restClient.post()
                .uri("/orders/print/invoice")
                .header("Authorization", authorization)
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .body(MAP_TYPE);
    }
}

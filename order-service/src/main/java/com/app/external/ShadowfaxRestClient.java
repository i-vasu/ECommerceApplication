package com.app.external;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.http.MediaType;

@Component
public class ShadowfaxRestClient implements ShadowfaxClient {

    private final RestClient restClient;
    private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE = new ParameterizedTypeReference<>() {
    };

    public ShadowfaxRestClient(@Value("${shadowfax.api.url:https://hlbackend.staging.shadowfax.in/}") String baseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    @Override
    public Map<String, Object> createOrder(String token, Map<String, Object> orderRequest) {
        return restClient.post()
                .uri("/api/v3/orders")
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(orderRequest)
                .retrieve()
                .body(MAP_TYPE);
    }

    @Override
    public Map<String, Object> trackOrder(String token, String awb) {
        return restClient.get()
                .uri("/api/v3/orders/{awb}", awb)
                .header("Authorization", token)
                .retrieve()
                .body(MAP_TYPE);
    }
}

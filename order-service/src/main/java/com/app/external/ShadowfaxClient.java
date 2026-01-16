package com.app.external;

import java.util.Map;

public interface ShadowfaxClient {

    Map<String, Object> createOrder(String token, Map<String, Object> orderRequest);

    Map<String, Object> trackOrder(String token, String awb);

}

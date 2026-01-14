package com.app.external;

import java.util.Map;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "shadowfax", url = "https://hlbackend.staging.shadowfax.in/")
public interface ShadowfaxClient {

    @PostMapping(value = "/api/v3/orders", consumes = "application/json")
    Map<String, Object> createOrder(@RequestHeader("Authorization") String token,
            @RequestBody Map<String, Object> orderRequest);

    @GetMapping(value = "/api/v3/orders/{awb}", consumes = "application/json")
    Map<String, Object> trackOrder(@RequestHeader("Authorization") String token, @PathVariable("awb") String awb);

}

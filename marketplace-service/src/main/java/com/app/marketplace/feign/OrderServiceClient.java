package com.app.marketplace.feign;

import com.app.marketplace.dto.external.ExternalOrderDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "ecommerce-service") // Name from order-service application.properties
public interface OrderServiceClient {

    @PostMapping("/api/internal/orders/ingest")
    ExternalOrderDTO ingestOrder(@RequestBody ExternalOrderDTO orderDTO);
}

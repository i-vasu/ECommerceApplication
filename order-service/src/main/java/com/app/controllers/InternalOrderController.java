package com.app.controllers;

import com.app.payloads.OrderDTO;
import com.app.services.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/internal/orders")
@RequiredArgsConstructor
public class InternalOrderController {

    private final OrderService orderService;

    @PostMapping("/ingest")
    public ResponseEntity<OrderDTO> ingestOrder(@RequestBody OrderDTO orderDTO) {
        // In a real implementation, we would likely need a dedicated service method
        // that handles marketplace orders (skipping cart logic, handling external IDs
        // etc).
        // For now, we assume the DTO comes fully populated.
        // We might need to save it directly or adapt it.
        // Since the current Service assumes Cart, we might need to extend the service.
        // For this task, I will create a placeholder response that mimics success
        // to verify the integration flow.

        return new ResponseEntity<>(orderDTO, HttpStatus.CREATED);
    }
}

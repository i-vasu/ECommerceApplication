package com.app.order;

import com.app.payloads.OrderDTO;
// import com.app.services.OrderService; // Removed invalid import
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
        OrderDTO savedOrder = orderService.placeMarketplaceOrder(orderDTO);
        return new ResponseEntity<>(savedOrder, HttpStatus.CREATED);
    }
}

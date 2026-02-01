package com.app.order.admin.controllers;

import com.app.order.order.OrderService;
import com.app.order.payloads.OrderDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
public class InternalOrderController implements InternalOrderApi {

    private final OrderService orderService;

    public InternalOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @Override
    public ResponseEntity<OrderDTO> ingestOrder(@RequestBody OrderDTO orderDTO) {
        OrderDTO savedOrder = orderService.placeMarketplaceOrder(orderDTO);
        return new ResponseEntity<>(savedOrder, HttpStatus.CREATED);
    }
}

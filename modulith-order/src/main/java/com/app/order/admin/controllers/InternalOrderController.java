package com.app.order.admin.controllers;

import com.app.order.order.OrderService;
import com.app.order.payloads.OrderDTO;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
public class InternalOrderController implements InternalOrderApi {

    private final OrderService orderService;

    public InternalOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @Override
    public ResponseEntity<OrderDTO> ingestOrder(@Valid @RequestBody OrderDTO orderDTO) {
        OrderDTO savedOrder = orderService.placeMarketplaceOrder(orderDTO);
        return new ResponseEntity<>(savedOrder, HttpStatus.CREATED);
    }
}


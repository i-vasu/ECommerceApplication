package com.app.admin.controllers;

import com.app.order.payloads.OrderDTO;
import com.app.order.order.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

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

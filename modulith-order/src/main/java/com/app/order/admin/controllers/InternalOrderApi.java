package com.app.order.admin.controllers;

import com.app.order.payloads.OrderDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Internal Order", description = "Admin Internal Order Management")
@SecurityRequirement(name = "E-Commerce Application")
public interface InternalOrderApi {

    @Operation(summary = "Ingest Order", description = "Ingests an order from disparate marketplaces")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Order ingested successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid order data")
    })
    @PostMapping("/ingest")
    ResponseEntity<OrderDTO> ingestOrder(@RequestBody OrderDTO orderDTO);
}

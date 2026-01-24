package com.app.order.order;

import com.app.order.payloads.OrderDTO;
import com.app.order.payloads.OrderResponse;
import com.app.core.payloads.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import java.util.List;

@Tag(name = "Order v1", description = "Order Management APIs - Version 1")
public interface OrderApi {

    @Operation(summary = "Place Order", description = "Places an order for a user from a specific cart")
    ResponseEntity<ApiResponse<OrderDTO>> orderProducts(String emailId, Long cartId, String paymentMethod);

    @Operation(summary = "Get All Orders", description = "Retrieves all orders (Admin only)")
    ResponseEntity<ApiResponse<OrderResponse>> getAllOrders(Integer pageNumber, Integer pageSize, String sortBy,
            String sortOrder);

    @Operation(summary = "Get Orders by User", description = "Retrieves all orders for a specific user")
    ResponseEntity<ApiResponse<List<OrderDTO>>> getOrdersByUser(String emailId);

    @Operation(summary = "Get Order Details", description = "Retrieves details of a specific order")
    ResponseEntity<ApiResponse<OrderDTO>> getOrderByUser(String emailId, Long orderId);

    @Operation(summary = "Update Order Status", description = "Updates the status of an order (Admin only)")
    ResponseEntity<ApiResponse<OrderDTO>> updateOrderByUser(String emailId, Long orderId, String orderStatus);
}

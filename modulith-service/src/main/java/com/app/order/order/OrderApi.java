package com.app.order.order;

import com.app.order.payloads.OrderDTO;
import com.app.order.payloads.OrderResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import java.util.List;

@Tag(name = "Order", description = "Order Management APIs")
public interface OrderApi {

    @Operation(summary = "Place Order", description = "Places an order for a user from a specific cart")
    ResponseEntity<OrderDTO> orderProducts(String emailId, Long cartId, String paymentMethod);

    @Operation(summary = "Get All Orders", description = "Retrieves all orders (Admin only)")
    ResponseEntity<OrderResponse> getAllOrders(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder);

    @Operation(summary = "Get Orders by User", description = "Retrieves all orders for a specific user")
    ResponseEntity<List<OrderDTO>> getOrdersByUser(String emailId);

    @Operation(summary = "Get Order Details", description = "Retrieves details of a specific order")
    ResponseEntity<OrderDTO> getOrderByUser(String emailId, Long orderId);

    @Operation(summary = "Update Order Status", description = "Updates the status of an order (Admin only)")
    ResponseEntity<OrderDTO> updateOrderByUser(String emailId, Long orderId, String orderStatus);
}

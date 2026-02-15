package com.app.order.order;

import com.app.order.payloads.OrderDTO;
import com.app.order.payloads.OrderRequest;
import com.app.order.payloads.OrderResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@Tag(name = "Order v1", description = "Customer and Admin Order Management APIs")
public interface OrderApi {

    @Operation(summary = "Place Order", description = "Finalizes the checkout process by converting a cart into a persistent order. Triggers inventory commitment and payment verification.")
    @ApiResponse(responseCode = "201", description = "Order placed successfully")
    @ApiResponse(responseCode = "400", description = "Invalid payment method or cart data")
    @ApiResponse(responseCode = "404", description = "User or Cart not found")
    ResponseEntity<com.app.core.payloads.ApiResponse<OrderDTO>> orderProducts(
            @Parameter(description = "Customer email") String emailId,
            @Parameter(description = "ID of the cart to purchase") Long cartId,
            @Parameter(description = "Payment method (CREDIT_CARD, RAZORPAY, COD, etc.)") String paymentMethod,
            @Parameter(description = "Unique request identifier to prevent duplicate orders", required = false) @org.springframework.web.bind.annotation.RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody(required = false) OrderRequest request);

    @Operation(summary = "List All Orders", description = "Retrieves a paginated list of all orders in the system. Restricted to ADMIN.")
    @ApiResponse(responseCode = "200", description = "Orders retrieved successfully")
    ResponseEntity<com.app.core.payloads.ApiResponse<OrderResponse>> getAllOrders(
            @Parameter(description = "Page number") Integer pageNumber,
            @Parameter(description = "Items per page") Integer pageSize,
            @Parameter(description = "Sort field") String sortBy,
            @Parameter(description = "Sort direction") String sortOrder);

    @Operation(summary = "User's Order History", description = "Retrieves all past orders for the authenticated user.")
    @ApiResponse(responseCode = "200", description = "Order history retrieved")
    ResponseEntity<com.app.core.payloads.ApiResponse<List<OrderDTO>>> getOrdersByUser(
            @Parameter(description = "Customer email") String emailId);

    @Operation(summary = "Order Details", description = "Retrieves full details of a specific order.")
    @ApiResponse(responseCode = "200", description = "Order found")
    @ApiResponse(responseCode = "404", description = "Order not found")
    ResponseEntity<com.app.core.payloads.ApiResponse<OrderDTO>> getOrderByUser(
            @Parameter(description = "Customer email") String emailId,
            @Parameter(description = "Reference order ID") Long orderId);

    @Operation(summary = "Update Order Status", description = "Changes the lifecycle state of an order (e.g., SHIPPED, DELIVERED). Restricted to ADMIN.")
    @ApiResponse(responseCode = "200", description = "Status updated successfully")
    ResponseEntity<com.app.core.payloads.ApiResponse<OrderDTO>> updateOrderByUser(
            @Parameter(description = "Customer email") String emailId,
            @Parameter(description = "Order ID") Long orderId,
            @Parameter(description = "New status string") String orderStatus);

    @Operation(summary = "Cancel Order", description = "Cancels a pending or processing order. Triggers inventory release and refund if applicable.")
    @ApiResponse(responseCode = "200", description = "Order cancelled successfully")
    @ApiResponse(responseCode = "400", description = "Order cannot be cancelled in its current state")
    ResponseEntity<com.app.core.payloads.ApiResponse<OrderDTO>> cancelOrder(
            @Parameter(description = "Customer email") String emailId,
            @Parameter(description = "Order ID to cancel") Long orderId);

    @Operation(summary = "Reorder Items", description = "Creates a new cart and populates it with items from a previous order for quick re-purchase.")
    @ApiResponse(responseCode = "200", description = "Items added to a new cart")
    ResponseEntity<com.app.core.payloads.ApiResponse<com.app.cart.payloads.CartDTO>> reorder(
            @Parameter(description = "Customer email") String emailId,
            @Parameter(description = "Order ID to replicate") Long orderId);
}

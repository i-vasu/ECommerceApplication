package com.app.order.order;

import com.app.core.constants.AppConstants;
import com.app.core.payloads.ApiResponse;
import com.app.core.version.ApiVersion;
import com.app.order.payloads.OrderDTO;
import com.app.order.payloads.OrderResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@ApiVersion(1)
@SecurityRequirement(name = "E-Commerce Application")
public class OrderController implements OrderApi {

	@Autowired
	public OrderService orderService;

	@PostMapping("/public/users/{emailId}/carts/{cartId}/payments/{paymentMethod}/order")
	@Override
	public ResponseEntity<ApiResponse<OrderDTO>> orderProducts(@PathVariable String emailId, @PathVariable Long cartId,
			@PathVariable String paymentMethod) {
		OrderDTO order = orderService.placeOrder(emailId, cartId, paymentMethod);

		return new ResponseEntity<>(ApiResponse.success(order, "Order placed successfully"), HttpStatus.CREATED);
	}

	@GetMapping("/admin/orders")
	@Override
	public ResponseEntity<ApiResponse<OrderResponse>> getAllOrders(
			@RequestParam(name = "pageNumber", defaultValue = AppConstants.PAGE_NUMBER, required = false) Integer pageNumber,
			@RequestParam(name = "pageSize", defaultValue = AppConstants.PAGE_SIZE, required = false) Integer pageSize,
			@RequestParam(name = "sortBy", defaultValue = AppConstants.SORT_ORDERS_BY, required = false) String sortBy,
			@RequestParam(name = "sortOrder", defaultValue = AppConstants.SORT_DIR, required = false) String sortOrder) {

		OrderResponse orderResponse = orderService.getAllOrders(pageNumber, pageSize, sortBy, sortOrder);

		return ResponseEntity.ok(ApiResponse.success(orderResponse, "Orders retrieved successfully"));
	}

	@GetMapping("public/users/{emailId}/orders")
	@Override
	public ResponseEntity<ApiResponse<List<OrderDTO>>> getOrdersByUser(@PathVariable String emailId) {
		List<OrderDTO> orders = orderService.getOrdersByUser(emailId);

		return ResponseEntity.ok(ApiResponse.success(orders, "User orders retrieved successfully"));
	}

	@GetMapping("public/users/{emailId}/orders/{orderId}")
	@Override
	public ResponseEntity<ApiResponse<OrderDTO>> getOrderByUser(@PathVariable String emailId,
			@PathVariable Long orderId) {
		OrderDTO order = orderService.getOrder(emailId, orderId);

		return ResponseEntity.ok(ApiResponse.success(order, "Order details retrieved successfully"));
	}

	@PutMapping("admin/users/{emailId}/orders/{orderId}/orderStatus/{orderStatus}")
	@Override
	public ResponseEntity<ApiResponse<OrderDTO>> updateOrderByUser(@PathVariable String emailId,
			@PathVariable Long orderId,
			@PathVariable String orderStatus) {
		OrderDTO order = orderService.updateOrder(emailId, orderId, orderStatus);

		return ResponseEntity.ok(ApiResponse.success(order, "Order status updated successfully"));
	}

}

package com.app.order.order;

import com.app.order.payloads.OrderDTO;
import com.app.order.payloads.OrderRequest;
import com.app.order.payloads.OrderResponse;

import java.util.List;

public interface OrderService {

	OrderDTO placeOrder(String emailId, Long cartId, String paymentMethod, OrderRequest request);

	OrderDTO getOrder(String emailId, Long orderId);

	OrderDTO getOrderById(Long orderId);

	List<OrderDTO> getOrdersByUser(String emailId);

	OrderResponse getAllOrders(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder);

	OrderDTO updateOrder(String emailId, Long orderId, String orderStatus);

	OrderDTO updateOrderStatusInternal(Long orderId, String orderStatus);

	/**
	 * Places an order from marketplace integration (Amazon, Flipkart, etc.)
	 * This bypasses the cart-based flow and creates an order directly from the DTO.
	 */
	OrderDTO placeMarketplaceOrder(OrderDTO orderDTO);

	/**
	 * Cancels an order and initiates associated refunds and logistics
	 * cancellations.
	 */
	OrderDTO cancelOrder(String emailId, Long orderId);


	void confirmPayment(Long orderId, String transactionId);

	void markPaymentFailed(Long orderId, String reason);

	void updateOrderStatus(Long orderId, String status);

	List<OrderDTO> findPendingOrdersByItemCode(String itemCode);

	void shipOrder(Long orderId);

	com.app.cart.payloads.CartDTO reorder(String emailId, Long orderId);
}

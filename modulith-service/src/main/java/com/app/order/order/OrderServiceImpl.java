package com.app.order.order;

import com.app.identity.UserService;
import com.app.cart.CartService;
import com.app.payment.PaymentService;
import com.app.shipping.ShipmentService;
import com.app.order.services.ERPNextService;
import com.app.product.ProductService;
import com.app.inventory.InventoryReservationService;
import com.app.product.payloads.ProductDTO;
import com.app.commerce.states.OrderStatus;
import com.app.commerce.states.OrderStateMachine;
import com.app.order.async.OrderProducer;
import com.app.core.async.EventProducer;

import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.List;
import java.util.ArrayList;

import com.app.order.mappers.OrderMapper;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import com.app.order.OrderCreatedEvent;
import com.app.core.APIException;
import com.app.core.ResourceNotFoundException;

import com.app.order.entities.Cart;
import com.app.order.entities.CartItem;
import com.app.order.repositories.CartRepo;
import com.app.order.entities.Order;
import com.app.order.entities.OrderItem;
import com.app.order.entities.Payment;
import com.app.product.entities.Product;
import com.app.order.payloads.OrderDTO;
import com.app.order.payloads.OrderItemDTO;
import com.app.order.payloads.OrderResponse;
import com.app.order.repositories.CartItemRepo;
import com.app.order.repositories.OrderRepo;
import com.app.order.repositories.OrderItemRepo;
import com.app.order.repositories.PaymentRepo;
import com.app.identity.repositories.UserRepo;
import com.app.product.repositories.ProductRepo;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class OrderServiceImpl implements OrderService {

	private final UserRepo userRepo;
	private final ApplicationEventPublisher eventPublisher;
	private final CartRepo cartRepo;
	private final OrderRepo orderRepo;
	private final PaymentRepo paymentRepo;
	private final OrderItemRepo orderItemRepo;
	private final CartItemRepo cartItemRepo;
	private final UserService userService;
	private final CartService cartService;
	private final ERPNextService erpNextService;
	private final OrderMapper orderMapper;
	private final PaymentService paymentService;
	private final ShipmentService shipmentService;
	private final InventoryReservationService inventoryReservationService;
	private final ProductRepo productRepo;
	private final OrderProducer orderProducer;
	private final EventProducer eventProducer;
	private final OrderStateMachine stateMachine;

	@Override
	@Transactional
	public OrderDTO placeOrder(String emailId, Long cartId, String paymentMethod) {

		var cart = cartRepo.findCartByEmailAndCartId(emailId, cartId);

		if (cart == null) {
			throw new ResourceNotFoundException("Cart", "cartId", cartId);
		}

		var cartItems = cart.getCartItems();
		if (cartItems.isEmpty()) {
			throw new APIException("Cart is empty");
		}

		List<CartItem> reservedItems = new ArrayList<>();

		try {
			for (var item : cartItems) {
				var itemCode = item.getItemCode();
				var reserved = inventoryReservationService.reserveStock(itemCode, item.getQuantity());
				if (reserved) {
					reservedItems.add(item);
				} else {
					for (var rBox : reservedItems) {
						inventoryReservationService.releaseStock(rBox.getItemCode(), rBox.getQuantity());
					}
					throw new APIException("Out of Stock (or Reservation Failed) for item: " + itemCode);
				}
			}
		} catch (Exception e) {
			for (var item : reservedItems) {
				inventoryReservationService.releaseStock(item.getItemCode(), item.getQuantity());
			}
			throw e;
		}

		var order = new Order();
		try {
			order.setEmail(emailId);
			order.setOrderDate(LocalDate.now());
			order.setTotalAmount(cart.getTotalPrice());
			order.setOrderStatus(OrderStatus.PENDING);

			var payment = new Payment();
			payment.setOrder(order);
			payment.setPaymentMethod(paymentMethod);

			var savedPayment = paymentRepo.save(payment);
			order.setPayment(savedPayment);

			var savedOrder = orderRepo.save(order);

			List<OrderItem> orderItemsList = new ArrayList<>();

			for (var cartItem : cartItems) {
				var orderItem = new OrderItem();

				var product = productRepo.findById(cartItem.getProductId())
						.orElseThrow(
								() -> new ResourceNotFoundException("Product", "productId", cartItem.getProductId()));

				orderItem.setProduct(product);
				orderItem.setItemCode(cartItem.getItemCode());
				orderItem.setProductName(cartItem.getProductName());
				orderItem.setQuantity(cartItem.getQuantity());
				orderItem.setDiscount(cartItem.getDiscount());
				orderItem.setOrderedPrice(cartItem.getProductPrice());
				orderItem.setOrder(savedOrder);
				orderItemsList.add(orderItem);
			}

			var savedOrderItems = orderItemRepo.saveAll(orderItemsList);
			savedOrder.setOrderItems(savedOrderItems);

			if (!"RAZORPAY".equalsIgnoreCase(paymentMethod)) {
				orderProducer.sendOrder(savedOrder.getOrderId());
			}

			cart.getCartItems().forEach(item -> {
				cartService.deleteProductFromCart(cartId, item.getProductId());
			});

			return orderMapper.orderToOrderDTO(savedOrder);

		} catch (Exception e) {
			for (var item : reservedItems) {
				inventoryReservationService.releaseStock(item.getItemCode(), item.getQuantity());
			}
			log.error("Order placement failed: {}", e.getMessage());
			throw new APIException("Order placement failed: " + e.getMessage());
		}
	}

	@Override
	public List<OrderDTO> getOrdersByUser(String emailId) {
		var orders = orderRepo.findAllByEmail(emailId);

		var orderDTOs = orders.stream()
				.map(orderMapper::orderToOrderDTO)
				.toList();

		if (orderDTOs.isEmpty()) {
			throw new APIException("No orders placed yet by the user with email: " + emailId);
		}

		return orderDTOs;
	}

	@Override
	public OrderDTO getOrder(String emailId, Long orderId) {

		var order = orderRepo.findOrderByEmailAndOrderId(emailId, orderId);

		if (order == null) {
			throw new ResourceNotFoundException("Order", "orderId", orderId);
		}

		return orderMapper.orderToOrderDTO(order);
	}

	@Override
	public OrderResponse getAllOrders(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder) {

		var sortByAndOrder = sortOrder.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending()
				: Sort.by(sortBy).descending();

		var pageDetails = PageRequest.of(pageNumber, pageSize, sortByAndOrder);
		var pageOrders = orderRepo.findAll(pageDetails);
		var orders = pageOrders.getContent();

		var orderDTOs = orders.stream()
				.map(orderMapper::orderToOrderDTO)
				.toList();

		if (orderDTOs.isEmpty()) {
			throw new APIException("No orders placed yet by the users");
		}

		var orderResponse = new OrderResponse();
		orderResponse.setContent(orderDTOs);
		orderResponse.setPageNumber(pageOrders.getNumber());
		orderResponse.setPageSize(pageOrders.getSize());
		orderResponse.setTotalElements(pageOrders.getTotalElements());
		orderResponse.setTotalPages(pageOrders.getTotalPages());
		orderResponse.setLastPage(pageOrders.isLast());

		return orderResponse;
	}

	@Override
	@Transactional
	public OrderDTO updateOrder(String emailId, Long orderId, String orderStatusStr) {
		var order = orderRepo.findOrderByEmailAndOrderId(emailId, orderId);
		if (order == null) {
			throw new ResourceNotFoundException("Order", "orderId", orderId);
		}

		try {
			var current = order.getOrderStatus();
			var next = OrderStatus.valueOf(orderStatusStr);

			stateMachine.transition(current, next);
			order.setOrderStatus(next);

		} catch (IllegalArgumentException e) {
			log.error("Invalid Status for Order ID {}: {}", orderId, orderStatusStr);
			throw new APIException("Invalid Status: " + orderStatusStr);
		}

		return orderMapper.orderToOrderDTO(order);
	}

	@Override
	@Transactional
	public OrderDTO placeMarketplaceOrder(OrderDTO orderDTO) {
		var order = new Order();
		order.setEmail(orderDTO.email());
		order.setOrderDate(LocalDate.now());
		order.setTotalAmount(orderDTO.totalAmount() != null ? orderDTO.totalAmount() : 0.0);

		try {
			order.setOrderStatus(
					orderDTO.orderStatus() != null
							? OrderStatus.valueOf(orderDTO.orderStatus())
							: OrderStatus.PENDING);
		} catch (Exception e) {
			order.setOrderStatus(OrderStatus.PENDING);
		}

		var payment = new Payment();
		payment.setOrder(order);
		payment.setPaymentMethod("Marketplace");
		var savedPayment = paymentRepo.save(payment);
		order.setPayment(savedPayment);

		var savedOrder = orderRepo.save(order);

		List<OrderItem> orderItemsList = new ArrayList<>();
		if (orderDTO.orderItems() != null) {
			for (var itemDTO : orderDTO.orderItems()) {
				var orderItem = new OrderItem();

				var prodId = itemDTO.product() != null ? itemDTO.product().productId() : null;
				var itemCode = itemDTO.product() != null ? itemDTO.product().itemCode() : null;

				Product product = null;

				if (prodId != null) {
					product = productRepo.findById(prodId)
							.orElseThrow(() -> new ResourceNotFoundException("Product", "productId", prodId));
				} else if (itemCode != null) {
					product = productRepo.findByItemCode(itemCode);
				}

				if (product != null) {
					orderItem.setProduct(product);
				} else {
					log.warn(
							"Marketplace Order Item Product Not Found for Item Code: {} (Order email: {}). Proceeding without linking to internal Product.",
							itemCode, order.getEmail());
				}

				orderItem.setProductName(
						itemDTO.product() != null ? itemDTO.product().productName() : "Unknown");
				orderItem.setQuantity(itemDTO.quantity());
				orderItem.setDiscount(itemDTO.discount());
				orderItem.setOrderedPrice(itemDTO.orderedProductPrice());
				orderItem.setOrder(savedOrder);
				orderItemsList.add(orderItem);
			}
		}

		var savedOrderItems = orderItemRepo.saveAll(orderItemsList);
		savedOrder.setOrderItems(savedOrderItems);

		orderProducer.sendOrder(savedOrder.getOrderId());

		return orderMapper.orderToOrderDTO(savedOrder);
	}

	@Override
	@Transactional
	public OrderDTO cancelOrder(String emailId, Long orderId) {
		var order = orderRepo.findOrderByEmailAndOrderId(emailId, orderId);

		if (order == null) {
			throw new ResourceNotFoundException("Order", "orderId", orderId);
		}

		if (!stateMachine.canCancel(order.getOrderStatus())) {
			throw new APIException("Order cannot be cancelled in state: " + order.getOrderStatus());
		}

		if (order.getShipment() != null) {
			try {
				shipmentService.cancelShipment(order.getShipment().getShipmentId());
			} catch (Exception e) {
				log.error("Error cancelling shipment {}: {}", order.getShipment().getShipmentId(), e.getMessage());
			}
		}

		if (order.getPayment() != null && "captured".equalsIgnoreCase(order.getPayment().getPgStatus())) {
			try {
				paymentService.initiateRefund(order.getPayment().getPgPaymentId(), null,
						"Customer requested cancellation");
			} catch (Exception e) {
				log.error("Error initiating refund for payment {}: {}", order.getPayment().getPgPaymentId(),
						e.getMessage());
			}
		}

		if (order.getErpNextOrderName() != null) {
			try {
				eventProducer.publish("cancellation_events", order.getErpNextOrderName());
			} catch (Exception e) {
				log.error("Error queuing cancellation for ERPNext Order {}: {}", order.getErpNextOrderName(),
						e.getMessage());
			}
		}

		order.setOrderStatus(OrderStatus.CANCELLED);
		var savedOrder = orderRepo.save(order);

		return orderMapper.orderToOrderDTO(savedOrder);
	}
}

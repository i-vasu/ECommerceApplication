package com.app.order;

import com.app.user.UserService;
import com.app.cart.CartService;
import com.app.payment.PaymentService;
import com.app.shipment.ShipmentService;
import com.app.services.ERPNextService;
import com.app.services.InventoryReservationService;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.app.entites.Cart;
import com.app.entites.CartItem;
import com.app.entites.Order;
import com.app.entites.OrderItem;
import com.app.entites.Payment;
import com.app.exceptions.APIException;
import com.app.exceptions.ResourceNotFoundException;
import com.app.payloads.OrderDTO;
import com.app.payloads.OrderItemDTO;
import com.app.payloads.OrderResponse;
import com.app.repositories.CartItemRepo;
import com.app.repositories.CartRepo;
import com.app.repositories.OrderItemRepo;
import com.app.repositories.OrderRepo;
import com.app.repositories.PaymentRepo;
import com.app.repositories.UserRepo;

import jakarta.transaction.Transactional;

@Transactional
@Service
public class OrderServiceImpl implements OrderService {

	@Autowired
	public UserRepo userRepo;

	@Autowired
	public CartRepo cartRepo;

	@Autowired
	public OrderRepo orderRepo;

	@Autowired
	private PaymentRepo paymentRepo;

	@Autowired
	public OrderItemRepo orderItemRepo;

	@Autowired
	public CartItemRepo cartItemRepo;

	@Autowired
	public UserService userService;

	@Autowired
	public CartService cartService;

	@Autowired
	public ERPNextService erpNextService;

	@Autowired
	public ModelMapper modelMapper;

	@Autowired
	private PaymentService paymentService;

	@Autowired
	private ShipmentService shipmentService;

	@Autowired
	private InventoryReservationService inventoryReservationService;

	@Override
	public OrderDTO placeOrder(String emailId, Long cartId, String paymentMethod) {

		Cart cart = cartRepo.findCartByEmailAndCartId(emailId, cartId);

		if (cart == null) {
			throw new ResourceNotFoundException("Cart", "cartId", cartId);
		}

		List<CartItem> cartItems = cart.getCartItems();
		if (cartItems.size() == 0) {
			throw new APIException("Cart is empty");
		}

		// 1. Reserve Stock Atomically (Redis)
		// We track successful reservations to rollback if a later one fails
		List<CartItem> reservedItems = new ArrayList<>();

		try {
			for (CartItem item : cartItems) {
				String itemCode = item.getItemCode();
				boolean reserved = inventoryReservationService.reserveStock(itemCode, item.getQuantity());

				// If not in Redis (-1), we skip reservation and rely on ERPNext check later
				// (less safe but keeps flowing)
				// Or we could force a fetch from ERPNext to populate Redis.
				// For now, if true or false (exception thrown inside service if false)
				if (reserved) {
					reservedItems.add(item);
				}
			}
		} catch (Exception e) {
			// Rollback successful reservations
			for (CartItem item : reservedItems) {
				inventoryReservationService.releaseStock(item.getItemCode(), item.getQuantity());
			}
			throw e; // Re-throw
		}

		// 2. Proceed with DB Order Creation
		Order order = new Order();
		try {
			order.setEmail(emailId);
			order.setOrderDate(LocalDate.now());

			order.setTotalAmount(cart.getTotalPrice());
			order.setOrderStatus("Order Accepted !");

			Payment payment = new Payment();
			payment.setOrder(order);
			payment.setPaymentMethod(paymentMethod);

			payment = paymentRepo.save(payment);

			order.setPayment(payment);

			Order savedOrder = orderRepo.save(order);

			List<OrderItem> orderItems = new ArrayList<>();

			for (CartItem cartItem : cartItems) {
				OrderItem orderItem = new OrderItem();

				orderItem.setProductId(cartItem.getProductId());
				orderItem.setItemCode(cartItem.getItemCode());
				orderItem.setProductName(cartItem.getProductName());
				orderItem.setQuantity(cartItem.getQuantity());
				orderItem.setDiscount(cartItem.getDiscount());
				orderItem.setOrderedProductPrice(cartItem.getProductPrice());
				orderItem.setOrder(savedOrder);
				orderItems.add(orderItem);
			}

			orderItems = orderItemRepo.saveAll(orderItems);
			savedOrder.setOrderItems(orderItems);

			if (!"RAZORPAY".equalsIgnoreCase(paymentMethod)) {
				new Thread(() -> {
					erpNextService.createSalesOrder(savedOrder);
				}).start();
			}

			cart.getCartItems().forEach(item -> {
				cartService.deleteProductFromCart(cartId, item.getProductId());
			});

			OrderDTO orderDTO = modelMapper.map(savedOrder, OrderDTO.class);
			orderItems.forEach(item -> orderDTO.getOrderItems().add(modelMapper.map(item, OrderItemDTO.class)));

			return orderDTO;

		} catch (Exception e) {
			// If DB save failed, release reservations
			for (CartItem item : reservedItems) {
				inventoryReservationService.releaseStock(item.getItemCode(), item.getQuantity());
			}
			throw new APIException("Order placement failed: " + e.getMessage());
		}
	}

	@Override
	public List<OrderDTO> getOrdersByUser(String emailId) {
		List<Order> orders = orderRepo.findAllByEmail(emailId);

		List<OrderDTO> orderDTOs = orders.stream().map(order -> modelMapper.map(order, OrderDTO.class))
				.collect(Collectors.toList());

		if (orderDTOs.size() == 0) {
			throw new APIException("No orders placed yet by the user with email: " + emailId);
		}

		return orderDTOs;
	}

	@Override
	public OrderDTO getOrder(String emailId, Long orderId) {

		Order order = orderRepo.findOrderByEmailAndOrderId(emailId, orderId);

		if (order == null) {
			throw new ResourceNotFoundException("Order", "orderId", orderId);
		}

		return modelMapper.map(order, OrderDTO.class);
	}

	@Override
	public OrderResponse getAllOrders(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder) {

		Sort sortByAndOrder = sortOrder.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending()
				: Sort.by(sortBy).descending();

		Pageable pageDetails = PageRequest.of(pageNumber, pageSize, sortByAndOrder);

		Page<Order> pageOrders = orderRepo.findAll(pageDetails);

		List<Order> orders = pageOrders.getContent();

		List<OrderDTO> orderDTOs = orders.stream().map(order -> modelMapper.map(order, OrderDTO.class))
				.collect(Collectors.toList());

		if (orderDTOs.size() == 0) {
			throw new APIException("No orders placed yet by the users");
		}

		OrderResponse orderResponse = new OrderResponse();

		orderResponse.setContent(orderDTOs);
		orderResponse.setPageNumber(pageOrders.getNumber());
		orderResponse.setPageSize(pageOrders.getSize());
		orderResponse.setTotalElements(pageOrders.getTotalElements());
		orderResponse.setTotalPages(pageOrders.getTotalPages());
		orderResponse.setLastPage(pageOrders.isLast());

		return orderResponse;
	}

	@Override
	public OrderDTO updateOrder(String emailId, Long orderId, String orderStatus) {

		Order order = orderRepo.findOrderByEmailAndOrderId(emailId, orderId);

		if (order == null) {
			throw new ResourceNotFoundException("Order", "orderId", orderId);
		}

		order.setOrderStatus(orderStatus);

		return modelMapper.map(order, OrderDTO.class);
	}

	@Override
	public OrderDTO placeMarketplaceOrder(OrderDTO orderDTO) {
		// Create order directly from DTO (marketplace flow - no cart)
		Order order = new Order();
		order.setEmail(orderDTO.getEmail());
		order.setOrderDate(LocalDate.now());
		order.setTotalAmount(orderDTO.getTotalAmount() != null ? orderDTO.getTotalAmount() : 0.0);
		order.setOrderStatus(
				orderDTO.getOrderStatus() != null ? orderDTO.getOrderStatus() : "Marketplace Order Received");

		// Create payment record
		Payment payment = new Payment();
		payment.setOrder(order);
		payment.setPaymentMethod("Marketplace"); // Generic payment method for marketplace orders
		payment = paymentRepo.save(payment);
		order.setPayment(payment);

		Order savedOrder = orderRepo.save(order);

		// Process order items
		List<OrderItem> orderItems = new ArrayList<>();
		if (orderDTO.getOrderItems() != null) {
			for (OrderItemDTO itemDTO : orderDTO.getOrderItems()) {
				OrderItem orderItem = new OrderItem();
				orderItem.setProductId(itemDTO.getProduct() != null ? itemDTO.getProduct().getProductId() : null);
				orderItem.setProductName(
						itemDTO.getProduct() != null ? itemDTO.getProduct().getProductName() : "Unknown");
				orderItem.setQuantity(itemDTO.getQuantity());
				orderItem.setDiscount(itemDTO.getDiscount());
				orderItem.setOrderedProductPrice(itemDTO.getOrderedProductPrice());
				orderItem.setOrder(savedOrder);
				orderItems.add(orderItem);
			}
		}

		orderItems = orderItemRepo.saveAll(orderItems);
		savedOrder.setOrderItems(orderItems);

		// Async ERPNext sync
		new Thread(() -> {
			erpNextService.createSalesOrder(savedOrder);
		}).start();

		OrderDTO result = modelMapper.map(savedOrder, OrderDTO.class);
		orderItems.forEach(item -> result.getOrderItems().add(modelMapper.map(item, OrderItemDTO.class)));

		return result;
	}

	@Override
	public OrderDTO cancelOrder(String emailId, Long orderId) {
		Order order = orderRepo.findOrderByEmailAndOrderId(emailId, orderId);

		if (order == null) {
			throw new ResourceNotFoundException("Order", "orderId", orderId);
		}

		if ("CANCELLED".equalsIgnoreCase(order.getOrderStatus())) {
			throw new APIException("Order is already cancelled");
		}

		// 1. Cancel in Shiprocket if shipment exists
		if (order.getShipment() != null) {
			try {
				shipmentService.cancelShipment(order.getShipment().getShipmentId());
			} catch (Exception e) {
				System.err.println(">>> Error cancelling shipment: " + e.getMessage());
			}
		}

		// 2. Initiate Refund in Razorpay if paid
		if (order.getPayment() != null && "captured".equalsIgnoreCase(order.getPayment().getPgStatus())) {
			try {
				paymentService.initiateRefund(order.getPayment().getPgPaymentId(), null,
						"Customer requested cancellation");
			} catch (Exception e) {
				System.err.println(">>> Error initiating refund: " + e.getMessage());
			}
		}

		// 3. Cancel in ERPNext
		if (order.getErpNextOrderName() != null) {
			try {
				erpNextService.cancelSalesOrder(order.getErpNextOrderName());
			} catch (Exception e) {
				System.err.println(">>> Error cancelling ERPNext order: " + e.getMessage());
			}
		}

		order.setOrderStatus("CANCELLED");
		Order savedOrder = orderRepo.save(order);

		return modelMapper.map(savedOrder, OrderDTO.class);
	}
}

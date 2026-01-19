package com.app.order.order;

import com.app.identity.UserService;
import com.app.cart.CartService;
import com.app.payment.PaymentService;
import com.app.shipping.ShipmentService;
import com.app.order.services.ERPNextService;
import com.app.product.ProductService;
import com.app.inventory.InventoryReservationService;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.app.order.mappers.OrderMapper;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import com.app.order.OrderCreatedEvent;
import com.app.core.APIException;
import com.app.core.ResourceNotFoundException;
import org.jspecify.annotations.NonNull;

import com.app.order.entites.Cart;
import com.app.order.entites.CartItem;
import com.app.order.repositories.CartRepo;
import com.app.order.entites.Order;
import com.app.order.entites.OrderItem;
import com.app.order.entites.Payment;
import com.app.product.entites.Product;
import com.app.order.payloads.OrderDTO;
import com.app.order.payloads.OrderItemDTO;
import com.app.order.payloads.OrderResponse;
import com.app.order.repositories.CartItemRepo;
import com.app.order.repositories.OrderRepo;
import com.app.order.repositories.OrderItemRepo;
import com.app.order.repositories.PaymentRepo;
import com.app.order.repositories.PaymentRepo;
import com.app.identity.repositories.UserRepo;
import com.app.product.repositories.ProductRepo;

import jakarta.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import java.io.IOException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@Transactional
@Service
public class OrderServiceImpl implements OrderService {

	@Autowired
	public UserRepo userRepo;

	@Autowired
	private ApplicationEventPublisher eventPublisher;

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
	private OrderMapper orderMapper;

	@Autowired
	private PaymentService paymentService;

	@Autowired
	private ShipmentService shipmentService;

	@Autowired
	private InventoryReservationService inventoryReservationService;

	@Autowired
	private ProductRepo productRepo;

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
		List<CartItem> reservedItems = new ArrayList<>();

		try {
			for (CartItem item : cartItems) {
				String itemCode = item.getItemCode();
				boolean reserved = inventoryReservationService.reserveStock(itemCode, item.getQuantity());
				if (reserved) {
					reservedItems.add(item);
				}
			}
		} catch (Exception e) {
			for (CartItem item : reservedItems) {
				inventoryReservationService.releaseStock(item.getItemCode(), item.getQuantity());
			}
			throw e;
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

				// Fetch Product
				Product product = productRepo.findById(cartItem.getProductId())
						.orElseThrow(
								() -> new ResourceNotFoundException("Product", "productId", cartItem.getProductId()));

				orderItem.setProduct(product);
				orderItem.setItemCode(cartItem.getItemCode());
				orderItem.setProductName(cartItem.getProductName());
				orderItem.setQuantity(cartItem.getQuantity());
				orderItem.setDiscount(cartItem.getDiscount());
				orderItem.setOrderedPrice(cartItem.getProductPrice());
				orderItem.setOrder(savedOrder);
				orderItems.add(orderItem);
			}

			orderItems = orderItemRepo.saveAll(orderItems);
			savedOrder.setOrderItems(orderItems);

			if (!"RAZORPAY".equalsIgnoreCase(paymentMethod)) {
				erpNextService.createSalesOrderAsync(savedOrder);
			}

			cart.getCartItems().forEach(item -> {
				cartService.deleteProductFromCart(cartId, item.getProductId());
			});

			OrderDTO orderDTO = orderMapper.orderToOrderDTO(savedOrder);
			orderItems.forEach(item -> orderDTO.getOrderItems().add(orderMapper.orderItemToOrderItemDTO(item)));

			eventPublisher.publishEvent(new OrderCreatedEvent(
					savedOrder.getOrderId(),
					savedOrder.getEmail(),
					savedOrder.getTotalAmount() != null ? java.math.BigDecimal.valueOf(savedOrder.getTotalAmount())
							: java.math.BigDecimal.ZERO));

			return orderDTO;

		} catch (Exception e) {
			for (CartItem item : reservedItems) {
				inventoryReservationService.releaseStock(item.getItemCode(), item.getQuantity());
			}
			throw new APIException("Order placement failed: " + e.getMessage());
		}
	}

	@Override
	public List<OrderDTO> getOrdersByUser(String emailId) {
		List<Order> orders = orderRepo.findAllByEmail(emailId);

		List<OrderDTO> orderDTOs = orders.stream().map(orderMapper::orderToOrderDTO)
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

		return orderMapper.orderToOrderDTO(order);
	}

	@Override
	public OrderResponse getAllOrders(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder) {

		Sort sortByAndOrder = sortOrder.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending()
				: Sort.by(sortBy).descending();

		Pageable pageDetails = PageRequest.of(pageNumber, pageSize, sortByAndOrder);

		Page<Order> pageOrders = orderRepo.findAll(pageDetails);

		List<Order> orders = pageOrders.getContent();

		List<OrderDTO> orderDTOs = orders.stream().map(orderMapper::orderToOrderDTO)
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

		return orderMapper.orderToOrderDTO(order);
	}

	@Override
	public OrderDTO placeMarketplaceOrder(OrderDTO orderDTO) {
		Order order = new Order();
		order.setEmail(orderDTO.getEmail());
		order.setOrderDate(LocalDate.now());
		order.setTotalAmount(orderDTO.getTotalAmount() != null ? orderDTO.getTotalAmount() : 0.0);
		order.setOrderStatus(
				orderDTO.getOrderStatus() != null ? orderDTO.getOrderStatus() : "Marketplace Order Received");

		Payment payment = new Payment();
		payment.setOrder(order);
		payment.setPaymentMethod("Marketplace");
		payment = paymentRepo.save(payment);
		order.setPayment(payment);

		Order savedOrder = orderRepo.save(order);

		List<OrderItem> orderItems = new ArrayList<>();
		if (orderDTO.getOrderItems() != null) {
			for (OrderItemDTO itemDTO : orderDTO.getOrderItems()) {
				OrderItem orderItem = new OrderItem();

				Long prodId = itemDTO.getProduct() != null ? itemDTO.getProduct().getProductId() : null;
				if (prodId != null) {
					Product product = productRepo.findById(prodId)
							.orElseThrow(() -> new ResourceNotFoundException("Product", "productId", prodId));
					orderItem.setProduct(product);
				}

				orderItem.setProductName(
						itemDTO.getProduct() != null ? itemDTO.getProduct().getProductName() : "Unknown");
				orderItem.setQuantity(itemDTO.getQuantity());
				orderItem.setDiscount(itemDTO.getDiscount());
				orderItem.setOrderedPrice(itemDTO.getOrderedProductPrice());
				orderItem.setOrder(savedOrder);
				orderItems.add(orderItem);
			}
		}

		orderItems = orderItemRepo.saveAll(orderItems);
		savedOrder.setOrderItems(orderItems);

		erpNextService.createSalesOrderAsync(savedOrder);

		OrderDTO result = orderMapper.orderToOrderDTO(savedOrder);
		orderItems.forEach(item -> result.getOrderItems().add(orderMapper.orderItemToOrderItemDTO(item)));

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

		if (order.getShipment() != null) {
			try {
				shipmentService.cancelShipment(order.getShipment().getShipmentId());
			} catch (Exception e) {
				System.err.println(">>> Error cancelling shipment: " + e.getMessage());
			}
		}

		if (order.getPayment() != null && "captured".equalsIgnoreCase(order.getPayment().getPgStatus())) {
			try {
				paymentService.initiateRefund(order.getPayment().getPgPaymentId(), null,
						"Customer requested cancellation");
			} catch (Exception e) {
				System.err.println(">>> Error initiating refund: " + e.getMessage());
			}
		}

		if (order.getErpNextOrderName() != null) {
			try {
				erpNextService.cancelSalesOrder(order.getErpNextOrderName());
			} catch (Exception e) {
				System.err.println(">>> Error cancelling ERPNext order: " + e.getMessage());
			}
		}

		order.setOrderStatus("CANCELLED");
		Order savedOrder = orderRepo.save(order);

		return orderMapper.orderToOrderDTO(savedOrder);
	}
}

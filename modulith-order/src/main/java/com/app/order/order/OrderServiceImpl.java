package com.app.order.order;

import com.app.cart.domain.CartService;
import com.app.cart.repositories.CartItemRepo;
import com.app.cart.repositories.CartRepo;
import com.app.catalog.entities.Product;
import com.app.catalog.repositories.ProductRepo;
import com.app.checkout.pipeline.OptimizedCheckoutService;
import com.app.core.APIException;
import com.app.core.ResourceNotFoundException;
import com.app.core.async.EventProducer;
import com.app.core.audit.AuditTrail;
import com.app.finance.payment.PaymentService;
import com.app.governance.states.OrderStatus;
import com.app.logistics.inventory.InventoryReservationService;
import com.app.order.async.OrderProducer;
import com.app.order.entities.Order;
import com.app.order.entities.OrderHistory;
import com.app.order.entities.OrderItem;
import com.app.order.mappers.OrderMapper;
import com.app.order.payloads.OrderDTO;
import com.app.order.payloads.OrderResponse;
import com.app.order.repositories.OrderHistoryRepo;
import com.app.order.repositories.OrderItemRepo;
import com.app.order.repositories.OrderRepo;
import com.app.security.entities.Address;
import com.app.security.repositories.AddressRepo;
import com.app.security.repositories.UserRepo;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.tracing.Tracer;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Log4j2
@Transactional(readOnly = true)
@Service
public class OrderServiceImpl implements OrderService {

	private final ApplicationEventPublisher eventPublisher;
	private final CartRepo cartRepo;
	private final OrderRepo orderRepo;
	private final UserRepo userRepo;

	private final OrderItemRepo orderItemRepo;
	private final CartItemRepo cartItemRepo;
	private final com.app.core.contracts.UserServiceContract userService;
	private final CartService cartService;
	private final OrderMapper orderMapper;
	private final PaymentService paymentService;

	private final InventoryReservationService inventoryReservationService;
	private final OptimizedCheckoutService optimizedCheckoutService;
	private final AddressRepo addressRepo;
	private final OrderHistoryRepo orderHistoryRepo;
	private final MeterRegistry meterRegistry;
	private final Tracer tracer;
	private final ProductRepo productRepo;
	private final OrderProducer orderProducer;
	private final EventProducer eventProducer;
	private final com.app.governance.states.OperationalStateMachineService operationalStateMachine;
	private final com.app.governance.rules.RuleEngineService ruleEngine;
	private final com.app.core.services.RedisLockService lockService;

	public OrderServiceImpl(ApplicationEventPublisher eventPublisher, CartRepo cartRepo,
			OrderRepo orderRepo, UserRepo userRepo, OrderItemRepo orderItemRepo, CartItemRepo cartItemRepo,
			com.app.core.contracts.UserServiceContract userService, CartService cartService, OrderMapper orderMapper,
			PaymentService paymentService,
			InventoryReservationService inventoryReservationService, OptimizedCheckoutService optimizedCheckoutService,
			AddressRepo addressRepo, OrderHistoryRepo orderHistoryRepo, MeterRegistry meterRegistry, Tracer tracer,
			ProductRepo productRepo, OrderProducer orderProducer,
			EventProducer eventProducer,
			com.app.governance.states.OperationalStateMachineService operationalStateMachine,
			com.app.governance.rules.RuleEngineService ruleEngine,
			com.app.core.services.RedisLockService lockService) {
		this.eventPublisher = eventPublisher;
		this.cartRepo = cartRepo;
		this.orderRepo = orderRepo;
		this.userRepo = userRepo;
		this.orderItemRepo = orderItemRepo;
		this.cartItemRepo = cartItemRepo;
		this.userService = userService;
		this.cartService = cartService;
		this.orderMapper = orderMapper;
		this.paymentService = paymentService;

		this.inventoryReservationService = inventoryReservationService;
		this.optimizedCheckoutService = optimizedCheckoutService;
		this.addressRepo = addressRepo;
		this.orderHistoryRepo = orderHistoryRepo;
		this.meterRegistry = meterRegistry;
		this.tracer = tracer;
		this.productRepo = productRepo;
		this.orderProducer = orderProducer;
		this.eventProducer = eventProducer;
		this.operationalStateMachine = operationalStateMachine;
		this.ruleEngine = ruleEngine;
		this.lockService = lockService;
	}

	private Counter checkoutSuccessCounter;
	private Counter checkoutFailureCounter;
	private Timer checkoutTimer;

	@jakarta.annotation.PostConstruct
	public void initMetrics() {
		this.checkoutSuccessCounter = Counter.builder("ecommerce.checkout.success")
				.description("Number of successful checkouts")
				.register(meterRegistry);
		this.checkoutFailureCounter = Counter.builder("ecommerce.checkout.failure")
				.description("Number of failed checkouts")
				.register(meterRegistry);
		this.checkoutTimer = Timer.builder("ecommerce.checkout.duration")
				.description("Time taken for checkout process")
				.register(meterRegistry);
	}

	@Override
	@Transactional
	@AuditTrail(action = "PLACE_ORDER")
	public OrderDTO placeOrder(String emailId, Long cartId, String paymentMethod) {
		return checkoutTimer.record(() -> {
			var span = tracer.nextSpan().name("placeOrder").start();
			try (var scope = tracer.withSpan(span)) {
				return executePlaceOrder(emailId, cartId, paymentMethod);
			} finally {
				span.end();
			}
		});
	}

	private OrderDTO executePlaceOrder(String emailId, Long cartId, String paymentMethod) {
		String lockKey = "placeOrder:" + cartId;
		boolean locked = lockService.tryLock(lockKey, java.time.Duration.ofMinutes(1));
		if (!locked) {
			throw new APIException("Order placement is already in progress for this cart.");
		}

		try {
			var user = userRepo.findByEmail(emailId)
					.orElseThrow(() -> new ResourceNotFoundException("User", "email", emailId));

			var cart = cartRepo.findCartByUserIdAndCartId(user.getUserId(), cartId);
			if (cart == null) {
				throw new ResourceNotFoundException("Cart", "cartId", cartId);
			}

			var cartItems = cart.getCartItems();
			if (cartItems.isEmpty()) {
				throw new APIException("Cart is empty");
			}

			// fetch address if available
			Address address = null;
			if (cart.getAddressId() != null) {
				address = addressRepo.findById(cart.getAddressId()).orElse(null);
			}

			// Use Optimized Checkout for parallel validations
			var checkoutResult = optimizedCheckoutService.processCheckout(cart, address);

			if (!checkoutResult.inventory().locked()) {
				throw new APIException("Out of Stock (or Reservation Failed)");
			}

			if (address != null && !checkoutResult.address().valid()) {
				throw new APIException("Invalid delivery address: " + checkoutResult.address().message());
			}

			// Price Change Detection
			if (!checkoutResult.priceDiscrepancies().isEmpty()) {
				checkoutFailureCounter.increment();
				log.warn("Price discrepancy detected during checkout for user {}: {}", emailId,
						checkoutResult.priceDiscrepancies());
				throw new APIException("Price has updated. Please review your cart: "
						+ String.join(", ", checkoutResult.priceDiscrepancies()));
			}

			var order = new Order();
			try {
				order.setEmail(emailId);
				order.setUserId(user.getUserId());
				order.setOrderDate(LocalDate.now());

				/**
				 * UBIQUITOUS LANGUAGE: Address Snapshot.
				 * Prevents loss of delivery information if the user changes their profile
				 * address later.
				 */
				if (address != null) {
					order.setShippingDestination(
							address.getStreet(),
							address.getCity(),
							address.getState(),
							address.getPincode(),
							address.getCountry());
				}

				// SET FINANCIAL SNAPSHOTS (Domain Correctness with BigDecimal)
				order.setSubTotal(cart.getTotalPrice());
				order.setTotalTax(java.math.BigDecimal.valueOf(checkoutResult.tax().totalAmount()));
				order.setShippingCost(java.math.BigDecimal.valueOf(checkoutResult.shipping().amount()));
				order.setTotalAmount(checkoutResult.finalAmount());

				// DOMAIN ACTION: Formally transition to PENDING via State Machine
				order.setOrderStatus(OrderStatus.PENDING);
				order.setCouponCode(cart.getCouponCode());

				var savedOrder = orderRepo.save(order);
				operationalStateMachine.triggerOrderEvent(savedOrder.getOrderId(),
						com.app.governance.states.OrderEvent.PLACE);

				var paymentResponse = paymentService.initiatePayment(savedOrder.getOrderId(), paymentMethod);
				savedOrder.setPaymentId(paymentResponse.paymentId());
				savedOrder = orderRepo.save(savedOrder);

				List<OrderItem> orderItemsList = new ArrayList<>();

				for (var cartItem : cartItems) {
					var orderItem = new OrderItem();

					var product = productRepo.findById(cartItem.getProductId())
							.orElseThrow(
									() -> new ResourceNotFoundException("Product", "productId",
											cartItem.getProductId()));

					orderItem.setProductId(product.getProductId());
					orderItem.setItemCode(cartItem.getItemCode());
					orderItem.setProductName(cartItem.getProductName());
					orderItem.setQuantity(cartItem.getQuantity());
					orderItem.setDiscount(cartItem.getDiscount());
					orderItem.setOrderedPrice(cartItem.getProductPrice());
					orderItem.setOrder(savedOrder);

					// CALCULATE & PERSIST LINE-ITEM TAXES
					List<com.app.order.entities.OrderItemTaxDetail> itemTaxes = new ArrayList<>();
					for (var taxComp : checkoutResult.tax().components()) {
						// Distribute total tax components proportionally across items
						java.math.BigDecimal cartTotal = cart.getTotalPrice();
						java.math.BigDecimal itemTotal = cartItem.getProductPrice()
								.multiply(java.math.BigDecimal.valueOf(cartItem.getQuantity()));
						java.math.BigDecimal itemRatio = cartTotal.compareTo(java.math.BigDecimal.ZERO) > 0
								? itemTotal.divide(cartTotal, 4, java.math.RoundingMode.HALF_UP)
								: java.math.BigDecimal.ZERO;

						var detail = new com.app.order.entities.OrderItemTaxDetail();
						detail.setOrderItem(orderItem);
						detail.setTaxName(taxComp.name());
						detail.setTaxRate(taxComp.rate());
						detail.setTaxAmount(java.math.BigDecimal.valueOf(taxComp.amount()).multiply(itemRatio));
						itemTaxes.add(detail);
					}
					orderItem.setTaxDetails(itemTaxes);

					orderItemsList.add(orderItem);
				}

				var savedOrderItems = orderItemRepo.saveAll(orderItemsList);
				savedOrder.setOrderItems(savedOrderItems);

				if (!"RAZORPAY".equalsIgnoreCase(paymentMethod)) {
					orderProducer.sendOrder(savedOrder.getOrderId());
				}

				// LOG INITIAL STATUS HISTORY
				var history = new OrderHistory();
				history.setOrder(savedOrder);
				history.setNewStatus(OrderStatus.PENDING);
				history.setUpdatedAt(LocalDateTime.now());
				history.setUpdatedBy("SYSTEM");
				history.setComments("Order initially placed via " + paymentMethod);
				orderHistoryRepo.save(history);

				cart.getCartItems().forEach(item -> {
					cartService.deleteProductFromCart(cartId, item.getProductId());
				});

				// DECOUPLED: Domain Event
				var eventItems = savedOrder.getOrderItems().stream()
						.map(item -> new com.app.core.events.OrderCreatedEvent.OrderItemData(
								item.getItemCode(),
								item.getQuantity(),
								item.getOrderedPrice()))
						.toList();

				eventPublisher.publishEvent(new com.app.core.events.OrderCreatedEvent(
						savedOrder.getOrderId(),
						savedOrder.getUserId(),
						savedOrder.getEmail(),
						savedOrder.getTotalAmount(),
						eventItems));

				checkoutSuccessCounter.increment();
				return orderMapper.orderToOrderDTO(savedOrder);

			} catch (Exception e) {
				checkoutFailureCounter.increment();
				for (var item : cartItems) {
					inventoryReservationService.releaseStock(item.getItemCode(), item.getQuantity());
				}
				log.error("Order placement failed: {}", e.getMessage());
				throw new APIException("Order placement failed: " + e.getMessage());
			}
		} finally {
			lockService.unlock(lockKey);
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
	public OrderDTO getOrderById(Long orderId) {
		var order = orderRepo.findById(orderId)
				.orElseThrow(() -> new ResourceNotFoundException("Order", "orderId", orderId));
		return orderMapper.orderToOrderDTO(order);
	}

	@Override
	public OrderResponse getAllOrders(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder) {
		if (pageSize > com.app.core.constants.AppConstants.MAX_PAGE_SIZE) {
			pageSize = com.app.core.constants.AppConstants.MAX_PAGE_SIZE;
		}

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
			OrderStatus current = order.getOrderStatus();
			OrderStatus next = OrderStatus.valueOf(orderStatusStr);
			// 2. Trigger Unified State Machine Event (Calculated via SpEL map)
			Map<String, com.app.governance.states.OrderEvent> eventMap = Map.of(
					"PAYMENT_CAPTURED", com.app.governance.states.OrderEvent.PAY,
					"PROCESSING", com.app.governance.states.OrderEvent.APPROVE,
					"SHIPPED", com.app.governance.states.OrderEvent.SHIP,
					"DELIVERED", com.app.governance.states.OrderEvent.DELIVER);

			com.app.governance.states.OrderEvent event = eventMap.get(orderStatusStr);
			if (event != null) {
				operationalStateMachine.triggerOrderEvent(orderId, event);
			}

			order.setOrderStatus(next);

			if (next == OrderStatus.DELIVERED) {
				order.setDeliveredDate(java.time.LocalDateTime.now());
			}

			// LOG HISTORY
			var history = new OrderHistory();
			history.setOrder(order);
			history.setOldStatus(current);
			history.setNewStatus(next);
			history.setUpdatedAt(LocalDateTime.now());
			history.setUpdatedBy("SYSTEM"); // Ideally fetch from SecurityContext
			history.setComments("Status updated via API");
			orderHistoryRepo.save(history);

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
		order.setTotalAmount(orderDTO.totalAmount() != null ? orderDTO.totalAmount() : java.math.BigDecimal.ZERO);

		try {
			order.setOrderStatus(
					orderDTO.orderStatus() != null
							? OrderStatus.valueOf(orderDTO.orderStatus())
							: OrderStatus.PENDING);
		} catch (Exception e) {
			order.setOrderStatus(OrderStatus.PENDING);
		}

		var savedOrder = orderRepo.save(order);
		var paymentResponse = paymentService.initiatePayment(savedOrder.getOrderId(), "Marketplace");
		savedOrder.setPaymentId(paymentResponse.paymentId());
		savedOrder = orderRepo.save(savedOrder);

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
					orderItem.setProductId(product.getProductId());
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

		// Transition to CANCELLED state via Global State Machine
		operationalStateMachine.triggerOrderEvent(orderId, com.app.governance.states.OrderEvent.CANCEL);
		order.setOrderStatus(OrderStatus.CANCELLED);
		var savedOrder = orderRepo.save(order);

		// Map items for the event to ensure decoupling
		var items = order.getOrderItems().stream()
				.map(item -> new com.app.core.events.OrderCancelledEvent.CancelledItem(item.getItemCode(),
						item.getQuantity()))
				.toList();

		// Publish Event for Decoupled cleanup (Logistics, Payment, Inventory, ERPNext)
		eventPublisher.publishEvent(new com.app.core.events.OrderCancelledEvent(
				order.getOrderId(),
				order.getUserId(),
				order.getTotalAmount().doubleValue(),
				"Customer requested cancellation",
				items));

		log.info("Order {} cancelled and event published for cleanup.", orderId);
		return orderMapper.orderToOrderDTO(savedOrder);
	}

	@Override
	@Transactional
	public OrderDTO retryOrderSync(Long orderId) {
		Order order = orderRepo.findById(orderId)
				.orElseThrow(() -> new ResourceNotFoundException("Order", "orderId", orderId));

		if (order.getOrderStatus() != OrderStatus.PAYMENT_CAPTURED
				&& order.getOrderStatus() != OrderStatus.PROCESSING) {
			throw new APIException("Retry sync is only allowed for paid or processing orders.");
		}

		log.info("Manually retrying ERPNext sync for order: {}", orderId);
		eventPublisher
				.publishEvent(new com.app.core.events.ERPNextSyncRequestEvent("ORDER", orderId, "SALES_ORDER_SYNC"));

		return orderMapper.orderToOrderDTO(order);
	}

	@Override
	@Transactional
	public OrderDTO updateOrderStatusInternal(Long orderId, String orderStatusStr) {
		var order = orderRepo.findById(orderId)
				.orElseThrow(() -> new ResourceNotFoundException("Order", "orderId", orderId));
		return updateOrder(order.getEmail(), orderId, orderStatusStr);
	}

	@Override
	@Transactional
	public void confirmPayment(Long orderId, String transactionId) {
		var order = orderRepo.findById(orderId)
				.orElseThrow(() -> new ResourceNotFoundException("Order", "orderId", orderId));
		order.setOrderStatus(OrderStatus.PAYMENT_CAPTURED);
		// order.setPaymentId(Long.valueOf(transactionId)); // Assuming transactionId is
		// numeric if needed, or ignored/logged.
		orderRepo.save(order);
	}

	@Override
	@Transactional
	public void markPaymentFailed(Long orderId, String reason) {
		orderRepo.findById(orderId).ifPresent(order -> {
			order.setOrderStatus(OrderStatus.CANCELLED);
			log.warn("Order {} payment failed. Reason: {}", orderId, reason);
			orderRepo.save(order);
		});
	}

	@Override
	@Transactional
	public void updateOrderStatus(Long orderId, String status) {
		updateOrderStatusInternal(orderId, status);
	}

	@Override
	public List<OrderDTO> findPendingOrdersByItemCode(String itemCode) {
		return orderRepo.findPendingOrdersByItemCode(itemCode).stream()
				.map(orderMapper::orderToOrderDTO)
				.toList();
	}
}

package com.app.order.order;

import com.app.cart.domain.CartService;
import com.app.cart.repositories.CartItemRepo;
import com.app.cart.repositories.CartRepo;
import com.app.catalog.entities.Product;
import com.app.catalog.repositories.ProductRepo;
import com.app.checkout.pipeline.OptimizedCheckoutService;
import com.app.core.APIException;
import com.app.core.ResourceNotFoundException;
import com.app.core.multitenancy.UserContext;
import com.app.core.async.EventProducer;
import com.app.core.audit.AuditTrail;
import com.app.core.contracts.CartContract;
import com.app.finance.payment.PaymentService;
import com.app.governance.states.OrderStatus;
import com.app.logistics.inventory.InventoryReservationService;
import com.app.order.async.OrderProducer;
import com.app.order.entities.Order;
import com.app.order.entities.OrderHistory;
import com.app.order.entities.OrderItem;
import com.app.order.mappers.OrderMapper;
import com.app.order.payloads.OrderDTO;
import com.app.order.payloads.OrderRequest;
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
import io.micrometer.tracing.Tracer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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

@Transactional(readOnly = true)
@Service
public class OrderServiceImpl implements OrderService {

	private static final Logger log = LogManager.getLogger(OrderServiceImpl.class);

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
	private final com.app.logistics.inventory.InventoryService inventoryService;
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
			InventoryReservationService inventoryReservationService,
			com.app.logistics.inventory.InventoryService inventoryService,
			OptimizedCheckoutService optimizedCheckoutService,
			AddressRepo addressRepo, OrderHistoryRepo orderHistoryRepo, MeterRegistry meterRegistry,
			org.springframework.beans.factory.ObjectProvider<Tracer> tracerProvider,
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
		this.inventoryService = inventoryService;
		this.optimizedCheckoutService = optimizedCheckoutService;
		this.addressRepo = addressRepo;
		this.orderHistoryRepo = orderHistoryRepo;
		this.meterRegistry = meterRegistry;
		this.tracer = tracerProvider.getIfAvailable();
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
		this.checkoutSuccessCounter = meterRegistry.counter("ecommerce.checkout.success");
		this.checkoutFailureCounter = meterRegistry.counter("ecommerce.checkout.failure");
		this.checkoutTimer = meterRegistry.timer("ecommerce.checkout.duration");
	}

	private void validateUserOwnership(String emailId) {
		String currentUserEmail = UserContext.getCurrentUserEmail();
		if (currentUserEmail != null && !currentUserEmail.equals(emailId)) {
			// Check if admin? For now simplify to strict ownership
			// If we had roles we'd check: if (!isAdmin && !emailMatch) throw...
			throw new APIException("Unauthorized access to order data");
		}
	}
 
	@Override
	@Transactional
	@AuditTrail(action = "PLACE_ORDER")
	public OrderDTO placeOrder(String emailId, Long cartId, String paymentMethod, OrderRequest request) {
		validateUserOwnership(emailId);
		return checkoutTimer.record(() -> {
			if (tracer != null) {
				var span = tracer.nextSpan().name("placeOrder").start();
				try (var scope = tracer.withSpan(span)) {
					return executePlaceOrder(emailId, cartId, paymentMethod, request);
				} finally {
					span.end();
				}
			} else {
				return executePlaceOrder(emailId, cartId, paymentMethod, request);
			}
		});
	}

	private com.app.cart.entities.Cart fetchAndValidateCart(com.app.security.entities.User user, Long cartId, OrderRequest request) {
		var cart = cartRepo.findCartByUserIdAndCartId(user.getUserId(), cartId);
		if (cart == null) {
			throw new ResourceNotFoundException("Cart", "cartId", cartId);
		}

		var cartItems = cart.getCartItems();
		if (cartItems.isEmpty()) {
			throw new APIException("Cart is empty");
		}

		// Apply coupon from request if provided
		if (request != null && request.getCouponCode() != null && !request.getCouponCode().isEmpty()) {
			cart.setCouponCode(request.getCouponCode());
			cartRepo.save(cart);
		}
		return cart;
	}

	private Address resolveAddress(com.app.cart.entities.Cart cart, OrderRequest request) {
		Address address = null;
		if (request != null) {
			if (request.getAddressId() != null) {
				address = addressRepo.findById(request.getAddressId()).orElse(null);
			} else if (request.getStreet() != null && !request.getStreet().isEmpty()) {
				address = new Address();
				address.setStreet(request.getStreet());
				address.setCity(request.getCity());
				address.setState(request.getState());
				address.setPincode(request.getPincode());
				address.setCountry(request.getCountry() != null ? request.getCountry() : "India");
				address.setReceiverPhoneNumber(request.getReceiverPhoneNumber());
				address.setBuildingName(request.getStreet()); // Default
			}
		}

		if (address == null && cart.getAddressId() != null) {
			address = addressRepo.findById(cart.getAddressId()).orElse(null);
		}
		return address;
	}

	private Order prepareOrderEntity(String emailId, com.app.security.entities.User user, com.app.cart.entities.Cart cart, 
			Address address, com.app.checkout.pipeline.OptimizedCheckoutService.CheckoutResult checkoutResult) {
		var order = new Order();
		order.setEmail(emailId);
		order.setUserId(user.getUserId());
		order.setOrderDate(LocalDate.now());

		if (address != null) {
			order.setShippingDestination(
					address.getStreet(),
					address.getCity(),
					address.getState(),
					address.getPincode(),
					address.getCountry(),
					address.getReceiverPhoneNumber());
		}

		// SET FINANCIAL SNAPSHOTS (Domain Correctness with BigDecimal)
		order.setSubTotal(cart.getTotalPrice());
		order.setTotalTax(checkoutResult.tax().totalAmount());
		order.setShippingCost(checkoutResult.shipping().amount());
		order.setTotalAmount(checkoutResult.finalAmount());

		// DOMAIN ACTION: Formally transition to PENDING via State Machine
		order.setOrderStatus(OrderStatus.PENDING);
		order.setCouponCode(cart.getCouponCode());
		return order;
	}

	private List<OrderItem> createOrderItems(com.app.cart.entities.Cart cart, Order savedOrder, 
			com.app.checkout.pipeline.OptimizedCheckoutService.CheckoutResult checkoutResult) {
		List<OrderItem> orderItemsList = new ArrayList<>();

		// Track distributed totals per tax component to handle rounding residuals
		Map<String, java.math.BigDecimal> distributedTaxPerComp = new java.util.HashMap<>();
		checkoutResult.tax().components().forEach(c -> distributedTaxPerComp.put(c.name(), java.math.BigDecimal.ZERO));

		int totalItems = cart.getCartItems().size();
		int currentIndex = 0;

		for (var cartItem : cart.getCartItems()) {
			currentIndex++;
			boolean isLastItem = (currentIndex == totalItems);
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
				java.math.BigDecimal itemTaxAmount;
				
				if (isLastItem) {
					// Residual: Exact Total minus what we already distributed
					itemTaxAmount = taxComp.amount().subtract(distributedTaxPerComp.get(taxComp.name()));
				} else {
					// Pro-rata distribution
					java.math.BigDecimal cartTotal = cart.getTotalPrice();
					java.math.BigDecimal itemTotal = cartItem.getProductPrice()
							.multiply(java.math.BigDecimal.valueOf(cartItem.getQuantity()));
					java.math.BigDecimal itemRatio = cartTotal.compareTo(java.math.BigDecimal.ZERO) > 0
							? itemTotal.divide(cartTotal, 8, java.math.RoundingMode.HALF_UP)
							: java.math.BigDecimal.ZERO;
					
					itemTaxAmount = taxComp.amount().multiply(itemRatio).setScale(2, java.math.RoundingMode.HALF_UP);
					distributedTaxPerComp.put(taxComp.name(), distributedTaxPerComp.get(taxComp.name()).add(itemTaxAmount));
				}

				var detail = new com.app.order.entities.OrderItemTaxDetail();
				detail.setOrderItem(orderItem);
				detail.setTaxName(taxComp.name());
				detail.setTaxRate(taxComp.rate());
				detail.setTaxAmount(itemTaxAmount);
				itemTaxes.add(detail);
			}
			orderItem.setTaxDetails(itemTaxes);

			orderItemsList.add(orderItem);
		}

		return orderItemRepo.saveAll(orderItemsList);
	}

	private OrderDTO executePlaceOrder(String emailId, Long cartId, String paymentMethod, OrderRequest request) {
		String lockKey = "placeOrder:" + cartId;
		boolean locked = lockService.tryLock(lockKey, java.time.Duration.ofMinutes(1));
		if (!locked) {
			throw new APIException("Order placement is already in progress for this cart.");
		}

		try {
			var user = userRepo.findByEmail(emailId)
					.orElseThrow(() -> new ResourceNotFoundException("User", "email", emailId));

			var cart = fetchAndValidateCart(user, cartId, request);
			var address = resolveAddress(cart, request);

			// Use Optimized Checkout for parallel validations
			CartContract cartContract = toContract(cart);
			var checkoutResult = optimizedCheckoutService.processCheckout(cartContract, address);

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

			var order = prepareOrderEntity(emailId, user, cart, address, checkoutResult);
			order.setInventoryLockId(checkoutResult.inventory().lockId());
			try {
				var savedOrder = orderRepo.save(order);
				operationalStateMachine.triggerOrderEvent(savedOrder.getOrderId(),
						com.app.governance.states.OrderEvent.PLACE);

				var paymentResponse = paymentService.initiatePayment(savedOrder.getOrderId(), paymentMethod);
				savedOrder.setPaymentId(paymentResponse.paymentId());
				savedOrder = orderRepo.save(savedOrder);

				savedOrder.setOrderItems(createOrderItems(cart, savedOrder, checkoutResult));
				
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
								item.getProductName(), // Added usage
								item.getQuantity(),
								item.getOrderedPrice()))
						.toList();

				eventPublisher.publishEvent(new com.app.core.events.OrderCreatedEvent(
						savedOrder.getOrderId(),
						savedOrder.getUserId(),
						savedOrder.getEmail(),
						savedOrder.getShippingReceiverPhone(), // Added phone
						savedOrder.getTotalAmount(),
						eventItems));

				checkoutSuccessCounter.increment();
				return orderMapper.orderToOrderDTO(savedOrder);

			} catch (Exception e) {
				checkoutFailureCounter.increment();
				for (var item : cart.getCartItems()) {
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
		validateUserOwnership(emailId);
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
		validateUserOwnership(emailId);
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
					"DELIVERED", com.app.governance.states.OrderEvent.DELIVER,
					"CANCELLED", com.app.governance.states.OrderEvent.CANCEL);

			com.app.governance.states.OrderEvent event = eventMap.get(orderStatusStr);
			if (event != null) {
				operationalStateMachine.triggerOrderEvent(orderId, event);
			}

			order.setOrderStatus(next);

			if (next == OrderStatus.DELIVERED) {
				order.setDeliveredDate(java.time.LocalDateTime.now());

				// PUBLISH OrderCompletedEvent for Verified Review Tracking & Marketing
				List<Long> productIds = order.getOrderItems().stream()
						.map(OrderItem::getProductId)
						.toList();

				eventPublisher.publishEvent(new com.app.core.events.OrderCompletedEvent(
						orderId,
						order.getUserId(),
						order.getEmail(),
						productIds,
						order.getTotalAmount().doubleValue()));
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

			if (next == OrderStatus.CANCELLED) {
				List<com.app.core.events.OrderCancelledEvent.CancelledItem> cancelledItems = order.getOrderItems().stream()
						.map(item -> new com.app.core.events.OrderCancelledEvent.CancelledItem(item.getItemCode(),
								item.getQuantity()))
						.toList();

				eventPublisher.publishEvent(new com.app.core.events.OrderCancelledEvent(
						orderId, order.getUserId(), order.getTotalAmount().doubleValue(),
						"Cancelled via API", cancelledItems));
			}

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
		validateUserOwnership(emailId);
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

		// Publish Event for Decoupled cleanup (Logistics, Payment, Inventory, Custom ERP)
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
	public com.app.cart.payloads.CartDTO reorder(String emailId, Long orderId) {
		validateUserOwnership(emailId);
		var order = orderRepo.findOrderByEmailAndOrderId(emailId, orderId);
		if (order == null) {
			throw new ResourceNotFoundException("Order", "orderId", orderId);
		}

		// Create a fresh cart
		var cartDTO = cartService.createCart();
		Long newCartId = cartDTO.cartId();

		// Populate it with items from the old order
		for (var item : order.getOrderItems()) {
			cartService.addProductToCart(newCartId, item.getProductId(), item.getItemCode(), item.getQuantity());
		}

		// Fetch the final state of the cart
		return cartService.getCartById(newCartId);
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
		
		// GAP-12: Re-verify stock before capturing payment (Overselling prevention)
		for (var item : order.getOrderItems()) {
			boolean stockAvailable = inventoryReservationService.checkAggregateStock(item.getItemCode(), item.getQuantity());
			if (!stockAvailable) {
				log.warn("Stock no longer available for order {} during payment confirmation. Item: {}", orderId, item.getItemCode());
				throw new APIException("Stock no longer available for item: " + item.getItemCode());
			}
		}

		// Permanent Deduction
		if (order.getInventoryLockId() != null) {
			for (var item : order.getOrderItems()) {
				// We assume warehouse 1 for now as per current checkout logic
				inventoryService.confirmStock(1L, 1L, item.getItemCode(), item.getQuantity(), order.getInventoryLockId());
			}
		}

		order.setOrderStatus(OrderStatus.PAYMENT_CAPTURED);
		orderRepo.save(order);
	}

	@Override
	@Transactional
	public void markPaymentFailed(Long orderId, String reason) {
		orderRepo.findById(orderId).ifPresent(order -> {
			// 1. Transition State
			operationalStateMachine.triggerOrderEvent(orderId, com.app.governance.states.OrderEvent.CANCEL);
			order.setOrderStatus(OrderStatus.CANCELLED);
			var savedOrder = orderRepo.save(order);
			
			log.warn("Order {} payment failed. Reason: {}", orderId, reason);

			// 2. Publish Event to release stock (Inventory Module) and notify others
			var items = order.getOrderItems().stream()
				.map(item -> new com.app.core.events.OrderCancelledEvent.CancelledItem(item.getItemCode(),
						item.getQuantity()))
				.toList();

			eventPublisher.publishEvent(new com.app.core.events.OrderCancelledEvent(
				order.getOrderId(),
				order.getUserId(),
				order.getTotalAmount().doubleValue(),
				"Payment Failed: " + reason,
				items));
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

	@Override
	@Transactional
	public void shipOrder(Long orderId) {
		Order order = orderRepo.findById(orderId)
				.orElseThrow(() -> new ResourceNotFoundException("Order", "orderId", orderId));

		if (order.getOrderStatus() == OrderStatus.CANCELLED || order.getOrderStatus() == OrderStatus.SHIPPED || order.getOrderStatus() == OrderStatus.DELIVERED) {
			throw new APIException("Order cannot be shipped in current status: " + order.getOrderStatus());
		}

		var shippingAddress = new com.app.core.events.ShipmentRequestedEvent.ShippingAddress(
				"Valued Customer",
				order.getShippingReceiverPhone(),
				order.getShippingStreet(),
				order.getShippingCity(),
				order.getShippingState(),
				order.getShippingCountry(),
				order.getShippingPincode());

		List<Long> productIds = order.getOrderItems().stream().map(OrderItem::getProductId).toList();
		Map<Long, Product> productMap = productRepo.findAllById(productIds).stream()
				.collect(java.util.stream.Collectors.toMap(Product::getProductId, p -> p));

		var items = order.getOrderItems().stream()
				.map(item -> {
					var p = productMap.get(item.getProductId());
					double w = (p != null) ? p.getKgWeight() : 0.5;
					double l = (p != null) ? p.getLengthCm() : 10.0;
					double wd = (p != null) ? p.getWidthCm() : 10.0;
					double h = (p != null) ? p.getHeightCm() : 10.0;

					return new com.app.core.events.ShipmentRequestedEvent.ShipmentItem(
							item.getProductName(),
							item.getItemCode(),
							item.getQuantity(),
							item.getOrderedPrice(),
							w, l, wd, h);
				})
				.toList();

		var shipmentEvent = new com.app.core.events.ShipmentRequestedEvent(
				order.getOrderId(),
				order.getEmail(),
				shippingAddress,
				items,
				order.getTotalAmount(),
				false
		);

		eventPublisher.publishEvent(shipmentEvent);
        
        // Trigger state machine
        operationalStateMachine.triggerOrderEvent(orderId, com.app.governance.states.OrderEvent.SHIP);
        order.setOrderStatus(OrderStatus.SHIPPED);
        orderRepo.save(order);
	}

	private CartContract toContract(com.app.cart.entities.Cart cart) {
		List<CartContract.CartItemContract> items = cart.getCartItems().stream()
				.map(item -> new CartContract.CartItemContract(
						item.getProductId(),
						item.getItemCode(),
						item.getProductName(),
						item.getQuantity(),
						item.getProductPrice(),
						item.getDiscount()))
				.toList();

		return new CartContract(
				cart.getCartId(),
				cart.getUserId(),
				cart.getTotalPrice(),
				cart.getCouponCode(),
				items);
	}
}

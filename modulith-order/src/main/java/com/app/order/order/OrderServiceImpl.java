package com.app.order.order;

import com.app.identity.UserService;
import com.app.cart.CartService;
import com.app.core.audit.AuditTrail;
import com.app.payment.PaymentService;
import com.app.shipping.ShipmentService;
import com.app.order.services.ERPNextService;
import com.app.product.ProductService;
import com.app.inventory.InventoryReservationService;
import com.app.product.payloads.ProductDTO;
import com.app.commerce.states.OrderStatus;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.Counter;
import io.micrometer.tracing.Tracer;
import com.app.commerce.states.OrderStateMachine;
import com.app.order.services.OptimizedCheckoutService;
import com.app.identity.repositories.AddressRepo;
import com.app.identity.entities.Address;
import com.app.order.entities.OrderHistory;
import com.app.order.repositories.OrderHistoryRepo;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
	private final OptimizedCheckoutService optimizedCheckoutService;
	private final AddressRepo addressRepo;
	private final OrderHistoryRepo orderHistoryRepo;
	private final MeterRegistry meterRegistry;
	private final Tracer tracer;

	private final Counter checkoutSuccessCounter;
	private final Counter checkoutFailureCounter;
	private final Timer checkoutTimer;

	public OrderServiceImpl(UserRepo userRepo, ApplicationEventPublisher eventPublisher, CartRepo cartRepo,
			OrderItemRepo orderItemRepo, OrderRepo orderRepo, PaymentRepo paymentRepo, ProductService productService,
			PaymentService paymentService, ShipmentService shipmentService,
			InventoryReservationService inventoryReservationService, ProductRepo productRepo,
			OrderProducer orderProducer, EventProducer eventProducer, OrderStateMachine stateMachine,
			OptimizedCheckoutService optimizedCheckoutService, AddressRepo addressRepo,
			OrderHistoryRepo orderHistoryRepo,
			MeterRegistry meterRegistry,
			Tracer tracer) {
		this.userRepo = userRepo;
		this.eventPublisher = eventPublisher;
		this.cartRepo = cartRepo;
		this.orderItemRepo = orderItemRepo;
		this.orderRepo = orderRepo;
		this.paymentRepo = paymentRepo;
		this.productService = productService;
		this.paymentService = paymentService;
		this.shipmentService = shipmentService;
		this.inventoryReservationService = inventoryReservationService;
		this.productRepo = productRepo;
		this.orderProducer = orderProducer;
		this.eventProducer = eventProducer;
		this.stateMachine = stateMachine;
		this.optimizedCheckoutService = optimizedCheckoutService;
		this.addressRepo = addressRepo;
		this.orderHistoryRepo = orderHistoryRepo;
		this.meterRegistry = meterRegistry;
		this.tracer = tracer;

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

		var cart = cartRepo.findCartByEmailAndCartId(emailId, cartId);

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
		var checkoutResult = optimizedCheckoutService.processCheckout(cart, address, cart.getCouponCode());

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
			order.setOrderDate(LocalDate.now());

			// SET FINANCIAL SNAPSHOTS (Domain Correctness)
			order.setSubTotal(cart.getTotalPrice());
			order.setTotalTax(checkoutResult.tax().totalAmount());
			order.setShippingCost(checkoutResult.shipping().amount());
			order.setTotalAmount(checkoutResult.finalAmount());

			order.setOrderStatus(OrderStatus.PENDING);
			order.setCouponCode(cart.getCouponCode());

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

				// CALCULATE & PERSIST LINE-ITEM TAXES
				List<com.app.order.entities.OrderItemTaxDetail> itemTaxes = new ArrayList<>();
				for (var taxComp : checkoutResult.tax().components()) {
					// Distribute total tax components proportionally across items
					double itemRatio = (cartItem.getProductPrice() * cartItem.getQuantity()) / cart.getTotalPrice();
					var detail = new com.app.order.entities.OrderItemTaxDetail();
					detail.setOrderItem(orderItem);
					detail.setTaxName(taxComp.name());
					detail.setTaxRate(taxComp.rate());
					detail.setTaxAmount(taxComp.amount() * itemRatio);
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
		if (pageSize > com.app.config.AppConstants.MAX_PAGE_SIZE) {
			pageSize = com.app.config.AppConstants.MAX_PAGE_SIZE;
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
			var current = order.getOrderStatus();
			var next = OrderStatus.valueOf(orderStatusStr);

			stateMachine.transition(current, next);
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

		// 1. Release Stock back to Inventory
		for (var item : order.getOrderItems()) {
			try {
				inventoryReservationService.releaseStock(item.getItemCode(), item.getQuantity());
			} catch (Exception e) {
				log.error("Failed to release stock for item {} on cancellation of order {}: {}",
						item.getItemCode(), orderId, e.getMessage());
			}
		}

		// 2. Handle Shipment Cancellation
		if (order.getShipment() != null) {
			try {
				shipmentService.cancelShipment(order.getShipment().getShipmentId());
			} catch (Exception e) {
				log.error("Error cancelling shipment {}: {}", order.getShipment().getShipmentId(), e.getMessage());
			}
		}

		// 3. Initiate Refund if payment was captured
		if (order.getPayment() != null && "captured".equalsIgnoreCase(order.getPayment().getPgStatus())) {
			try {
				paymentService.initiateRefund(order.getPayment().getPgPaymentId(), null,
						"Customer requested cancellation");
			} catch (Exception e) {
				log.error("Error initiating refund for payment {}: {}", order.getPayment().getPgPaymentId(),
						e.getMessage());
			}
		}

		// 4. Notify ERPNext
		if (order.getErpNextOrderName() != null) {
			try {
				erpNextService.cancelSalesOrder(order.getErpNextOrderName());
			} catch (Exception e) {
				log.error("Error cancelling ERPNext Order {}: {}", order.getErpNextOrderName(),
						e.getMessage());
			}
		}

		order.setOrderStatus(OrderStatus.CANCELLED);
		var savedOrder = orderRepo.save(order);

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
		erpNextService.createSalesOrderAsync(order);

		return orderMapper.orderToOrderDTO(order);
	}
}

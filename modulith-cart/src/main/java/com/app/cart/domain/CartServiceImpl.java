package com.app.cart.domain;

import com.app.cart.domain.services.CartCouponService;
import com.app.cart.entities.Cart;
import com.app.cart.entities.CartItem;
import com.app.cart.payloads.CartDTO;
import com.app.cart.repositories.CartItemRepo;
import com.app.cart.repositories.CartRepo;
import com.app.catalog.ProductService;
import com.app.catalog.payloads.ProductDTO;
import com.app.core.APIException;
import com.app.core.ResourceNotFoundException;
import com.app.core.services.RedisLockService;
import com.app.finance.pricing.OrderTotalService;
import com.app.finance.pricing.contracts.OrderTotalInput;
import com.app.governance.rules.RuleEngineService;
import com.app.governance.states.OperationalStateMachineService;
import com.app.intelligence.analysis.services.AnalyticsService;
import com.app.logistics.inventory.InventoryReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Log4j2
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class CartServiceImpl implements CartService {

	private final CartRepo cartRepo;
	private final ProductService productService;
	private final CartItemRepo cartItemRepo;
	private final OrderTotalService orderTotalService;
	private final InventoryReservationService inventoryReservationService;
	private final CartCouponService cartCouponService;
	private final AnalyticsService analyticsService;
	private final RuleEngineService ruleEngine;
	private final OperationalStateMachineService stateMachineService;
	private final RedisLockService lockService;

	@Override
	@Transactional
	public CartDTO createCart() {
		var cart = new Cart();
		cart.setTotalPrice(java.math.BigDecimal.ZERO);
		var savedCart = cartRepo.save(cart);
		return new CartDTO(savedCart.getCartId(), savedCart.getTotalPrice(), List.of());
	}

	@Override
	@Transactional
	public CartDTO addProductToCart(Long cartId, Long productId, String itemCode, Integer quantity) {
		String lockKey = "cart:" + cartId;
		boolean locked = lockService.tryLock(lockKey, java.time.Duration.ofMinutes(1));
		if (!locked) {
			throw new APIException("Cart update is already in progress.");
		}

		try {
			var cart = cartRepo.findById(cartId)
					.orElseThrow(() -> new ResourceNotFoundException("Cart", "cartId", cartId));

			// Dynamic Capacity Check via SpEL
			Map<String, Object> context = new java.util.HashMap<>();
			context.put("itemCount", cart.getCartItems().size());

			if (!ruleEngine.evaluate("itemCount < 50", context)) {
				throw new APIException("Cart limit reached: Maximum 50 unique items allowed.");
			}

			var product = productService.getProductById(productId);
			if (product == null)
				throw new ResourceNotFoundException("Product", "productId", productId);

			var effectiveItemCode = (itemCode != null && !itemCode.isEmpty()) ? itemCode : product.itemCode();

			var cartItem = cartItemRepo.findCartItemByProductIdAndCartIdAndItemCode(cartId, productId,
					effectiveItemCode);

			if (cartItem != null && cartItem.getItemCode().equals(effectiveItemCode)) {
				throw new APIException("Item with code " + effectiveItemCode + " already exists in the cart");
			}

			// Use Redis Check (Async Write-Behind compatible)
			if (!inventoryReservationService.checkStock(effectiveItemCode, quantity)) {
				throw new APIException("Insufficient stock for " + effectiveItemCode);
			}

			var newCartItem = new CartItem();
			newCartItem.setProductId(product.productId());
			newCartItem.setProductName(product.productName());
			newCartItem.setItemCode(effectiveItemCode);
			newCartItem.setCart(cart);
			newCartItem.setQuantity(quantity);
			newCartItem.setDiscount(java.math.BigDecimal.valueOf(product.discount()));
			newCartItem.setProductPrice(java.math.BigDecimal.valueOf(product.specialPrice()));

			cartItemRepo.save(newCartItem);

			// Track in Analytics
			analyticsService.trackAddToCart(product.productId(), effectiveItemCode,
					java.math.BigDecimal.valueOf(product.specialPrice()));

			// To be safe, reload or ensure consistency for calculation.
			cart = cartRepo.findById(cartId).orElse(cart);

			var input = OrderTotalInput.builder()
					.id(cart.getCartId())
					.userId(cart.getUserId())
					.couponCode(cart.getCouponCode())
					.items(cart.getCartItems().stream().map(item -> new OrderTotalInput.ItemInput(
							item.getProductId(),
							item.getItemCode(),
							item.getProductPrice().doubleValue(),
							item.getQuantity())).toList())
					.build();

			var summary = orderTotalService.calculate(input);
			cart.setTotalPrice(summary.getFinalTotal());
			cartRepo.save(cart);

			var products = getCartProducts(cart);
			return new CartDTO(cart.getCartId(), cart.getTotalPrice(), products);
		} finally {
			lockService.unlock(lockKey);
		}
	}

	@Override
	public Page<CartDTO> getAllCarts(Pageable pageable) {
		var carts = cartRepo.findAll(pageable);
		return carts.map(cart -> {
			var products = getCartProducts(cart);
			return new CartDTO(cart.getCartId(), cart.getTotalPrice(), products);
		});
	}

	@Override
	public CartDTO getCart(Long userId, Long cartId) {
		var cart = cartRepo.findCartByUserIdAndCartId(userId, cartId);
		if (cart == null)
			throw new ResourceNotFoundException("Cart", "cartId", cartId);

		// Optional: Recalculate on View to ensure freshness
		var input = OrderTotalInput.builder()
				.id(cart.getCartId())
				.userId(cart.getUserId())
				.couponCode(cart.getCouponCode())
				.items(cart.getCartItems().stream().map(item -> new OrderTotalInput.ItemInput(
						item.getProductId(),
						item.getItemCode(),
						item.getProductPrice().doubleValue(),
						item.getQuantity())).toList())
				.build();

		var summary = orderTotalService.calculate(input);
		cart.setTotalPrice(summary.getFinalTotal());

		var products = getCartProducts(cart);
		return new CartDTO(cart.getCartId(), cart.getTotalPrice(), products);
	}

	@Override
	@Transactional
	public void updateProductInCarts(Long cartId, Long productId) {
		// Logic to update price if product changed globally
		var cartItem = cartItemRepo.findCartItemByProductIdAndCartId(cartId, productId);
		if (cartItem != null) {
			var product = productService.getProductById(productId);
			if (product != null) {
				cartItem.setProductPrice(java.math.BigDecimal.valueOf(product.specialPrice()));
				cartItemRepo.save(cartItem);

				// Recalculate Cart Total
				var cart = cartItem.getCart();
				var input = OrderTotalInput.builder()
						.id(cart.getCartId())
						.userId(cart.getUserId())
						.couponCode(cart.getCouponCode())
						.items(cart.getCartItems().stream().map(item -> new OrderTotalInput.ItemInput(
								item.getProductId(),
								item.getItemCode(),
								item.getProductPrice().doubleValue(),
								item.getQuantity())).toList())
						.build();

				var summary = orderTotalService.calculate(input);
				cart.setTotalPrice(summary.getFinalTotal());
				cartRepo.save(cart);
			}
		}
	}

	@Override
	@Transactional
	public CartDTO updateProductQuantityInCart(Long cartId, Long productId, String itemCode, Integer quantity) {
		var cart = cartRepo.findById(cartId)
				.orElseThrow(() -> new ResourceNotFoundException("Cart", "cartId", cartId));

		var cartItem = cartItemRepo.findCartItemByProductIdAndCartId(cartId, productId);
		if (cartItem == null)
			throw new ResourceNotFoundException("CartItem", "productId", productId);

		var effectiveItemCode = (itemCode != null && !itemCode.isEmpty()) ? itemCode : cartItem.getItemCode();

		if (!inventoryReservationService.checkStock(effectiveItemCode, quantity)) {
			throw new APIException("Insufficient stock for " + effectiveItemCode);
		}

		cartItem.setQuantity(quantity);
		cartItem.setItemCode(effectiveItemCode);
		cartItemRepo.save(cartItem);

		// Recalculate Pipeline
		var input = OrderTotalInput.builder()
				.id(cart.getCartId())
				.userId(cart.getUserId())
				.couponCode(cart.getCouponCode())
				.items(cart.getCartItems().stream().map(item -> new OrderTotalInput.ItemInput(
						item.getProductId(),
						item.getItemCode(),
						item.getProductPrice().doubleValue(),
						item.getQuantity())).toList())
				.build();

		var summary = orderTotalService.calculate(input);
		cart.setTotalPrice(summary.getFinalTotal());
		cartRepo.save(cart);

		var products = getCartProducts(cart);
		return new CartDTO(cart.getCartId(), cart.getTotalPrice(), products);
	}

	@Override
	@Transactional
	public String deleteProductFromCart(Long cartId, Long productId) {
		var cart = cartRepo.findById(cartId)
				.orElseThrow(() -> new ResourceNotFoundException("Cart", "cartId", cartId));
		var cartItem = cartItemRepo.findCartItemByProductIdAndCartId(cartId, productId);
		if (cartItem == null)
			throw new ResourceNotFoundException("Product", "productId", productId);

		cartItemRepo.delete(cartItem);

		// Remove from list for calculation accuracy if not automatically synched
		cart.getCartItems().remove(cartItem);

		// Recalculate Pipeline
		var input = OrderTotalInput.builder()
						.id(cart.getCartId())
						.userId(cart.getUserId())
						.couponCode(cart.getCouponCode())
				.items(cart.getCartItems().stream().map(item -> new OrderTotalInput.ItemInput(
						item.getProductId(),
						item.getItemCode(),
						item.getProductPrice().doubleValue(),
						item.getQuantity())).toList())
				.build();

		var summary = orderTotalService.calculate(input);
		cart.setTotalPrice(summary.getFinalTotal());
		cartRepo.save(cart);

		return "Product removed from the cart";
	}

	@Override
	@Transactional
	public CartDTO applyCoupon(Long cartId, String couponCode) {
		var cart = cartRepo.findById(cartId)
				.orElseThrow(() -> new ResourceNotFoundException("Cart", "cartId", cartId));

		// 1. Explicit Validation via CartCouponService
		var currentSubtotal = cart.getCartItems().stream()
				.mapToDouble(item -> item.getProductPrice().doubleValue() * item.getQuantity())
				.sum();
		cartCouponService.applyCouponToCart(couponCode, currentSubtotal);

		// 2. Persist code
		cart.setCouponCode(couponCode);

		// 3. Recalculate using full pricing pipeline
		var input = OrderTotalInput.builder()
				.id(cart.getCartId())
				.userId(cart.getUserId())
				.couponCode(cart.getCouponCode())
				.items(cart.getCartItems().stream().map(item -> new OrderTotalInput.ItemInput(
						item.getProductId(),
						item.getItemCode(),
						item.getProductPrice().doubleValue(),
						item.getQuantity())).toList())
				.build();

		var summary = orderTotalService.calculate(input);
		cart.setTotalPrice(summary.getFinalTotal());
		cartRepo.save(cart);

		var products = getCartProducts(cart);
		return new CartDTO(cart.getCartId(), cart.getTotalPrice(), products);
	}

	@Override
	@Transactional
	public CartDTO removeCoupon(Long cartId) {
		var cart = cartRepo.findById(cartId)
				.orElseThrow(() -> new ResourceNotFoundException("Cart", "cartId", cartId));

		cart.setCouponCode(null);

		// Recalculate
		var input = OrderTotalInput.builder()
				.id(cart.getCartId())
				.userId(cart.getUserId())
				.couponCode(cart.getCouponCode())
				.items(cart.getCartItems().stream().map(item -> new OrderTotalInput.ItemInput(
						item.getProductId(),
						item.getItemCode(),
						item.getProductPrice().doubleValue(),
						item.getQuantity())).toList())
				.build();

		var summary = orderTotalService.calculate(input);
		cart.setTotalPrice(summary.getFinalTotal());
		cartRepo.save(cart);

		var products = getCartProducts(cart);
		return new CartDTO(cart.getCartId(), cart.getTotalPrice(), products);
	}

	private List<ProductDTO> getCartProducts(Cart cart) {
		return cart.getCartItems().stream()
				.map(p -> productService.getProductById(p.getProductId()))
				.toList();
	}
}

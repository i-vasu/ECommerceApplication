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
import com.app.security.AddressService;
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
	private final AddressService addressService;

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
			newCartItem.setDiscount(product.discount());
			newCartItem.setProductPrice(product.specialPrice());

			cartItemRepo.save(newCartItem);

			// Track in Analytics
			analyticsService.trackAddToCart(product.productId(), effectiveItemCode,
					product.specialPrice());

			// To be safe, reload or ensure consistency for calculation.
			cart = cartRepo.findById(cartId).orElse(cart);

			recalculateCartTotals(cart);
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

		recalculateCartTotals(cart);

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
				cartItem.setProductPrice(product.specialPrice());
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
								item.getProductPrice(),
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

		recalculateCartTotals(cart);
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
						item.getProductPrice(),
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
				.map(item -> item.getProductPrice().multiply(java.math.BigDecimal.valueOf(item.getQuantity())))
				.reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
		cartCouponService.applyCouponToCart(couponCode, currentSubtotal);

		// 2. Persist code
		cart.setCouponCode(couponCode);

		recalculateCartTotals(cart);
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

		recalculateCartTotals(cart);
		cartRepo.save(cart);

		var products = getCartProducts(cart);
		return new CartDTO(cart.getCartId(), cart.getTotalPrice(), products);
	}

	@Override
	@Transactional
	public CartDTO mergeCarts(Long guestCartId, Long userId) {
		var guestCart = cartRepo.findById(guestCartId).orElse(null);
		if (guestCart == null) {
			// No guest cart to merge, just return user's cart if exists
			var userCart = cartRepo.findByUserId(userId).orElse(null);
			if (userCart != null) {
				return getCart(userId, userCart.getCartId());
			}
			return null; 
		}

		var userCart = cartRepo.findByUserId(userId).orElse(null);

		if (userCart == null) {
			// User has no cart, simply assign guest cart to user
			guestCart.setUserId(userId);
			recalculateCartTotals(guestCart);
			cartRepo.save(guestCart);
			return getCart(userId, guestCart.getCartId());
		}

		// User has a cart, need to merge items
		for (var guestItem : guestCart.getCartItems()) {
			var existingItem = cartItemRepo.findCartItemByProductIdAndCartIdAndItemCode(
					userCart.getCartId(), 
					guestItem.getProductId(), 
					guestItem.getItemCode());

			if (existingItem != null) {
				// Item exists, update quantity
				existingItem.setQuantity(existingItem.getQuantity() + guestItem.getQuantity());
				cartItemRepo.save(existingItem);
			} else {
				// Item doesn't exist, move it to user cart
				// We need to create a new item copy because re-parenting managed entities can be tricky with cascade
				var newItem = new CartItem();
				newItem.setCart(userCart);
				newItem.setProductId(guestItem.getProductId());
				newItem.setItemCode(guestItem.getItemCode());
				newItem.setProductName(guestItem.getProductName());
				newItem.setQuantity(guestItem.getQuantity());
				newItem.setProductPrice(guestItem.getProductPrice());
				newItem.setDiscount(guestItem.getDiscount());
				cartItemRepo.save(newItem);
			}
		}

		// Delete guest cart after merge
		cartRepo.delete(guestCart);

		// Recalculate User Cart
		return getCart(userId, userCart.getCartId());
	}

	private List<ProductDTO> getCartProducts(Cart cart) {
		return cart.getCartItems().stream()
				.map(p -> productService.getProductById(p.getProductId()))
				.toList();
	}

	@Override
	@Transactional
	public CartDTO updateCartAddress(Long cartId, Long addressId) {
		var cart = cartRepo.findById(cartId)
				.orElseThrow(() -> new ResourceNotFoundException("Cart", "cartId", cartId));

		cart.setAddressId(addressId);

		recalculateCartTotals(cart);
		cartRepo.save(cart);

		var products = getCartProducts(cart);
		return new CartDTO(cart.getCartId(), cart.getTotalPrice(), products);
	}

	private void recalculateCartTotals(Cart cart) {
		var inputBuilder = OrderTotalInput.builder()
				.id(cart.getCartId())
				.userId(cart.getUserId())
				.couponCode(cart.getCouponCode())
				.items(cart.getCartItems().stream().map(item -> new OrderTotalInput.ItemInput(
						item.getProductId(),
						item.getItemCode(),
						item.getProductPrice(),
						item.getQuantity())).toList());

		if (cart.getAddressId() != null) {
			try {
				var addr = addressService.getAddress(cart.getAddressId());
				if (addr != null) {
					inputBuilder.shippingState(addr.state());
					inputBuilder.shippingZip(addr.pincode());
					inputBuilder.shippingCountry(addr.country());
				}
			} catch (Exception e) {
				log.warn("Failed to fetch address {} for cart pricing: {}", cart.getAddressId(), e.getMessage());
			}
		}

		var summary = orderTotalService.calculate(inputBuilder.build());
		cart.setTotalPrice(summary.getFinalTotal());
	}
}

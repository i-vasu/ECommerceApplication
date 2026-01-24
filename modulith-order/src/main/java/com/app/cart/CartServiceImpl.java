package com.app.cart;

import java.util.List;
import com.app.cart.mappers.CartMapper;
import com.app.cart.services.CartCouponService;
import com.app.product.ProductService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.app.order.entities.Cart;
import com.app.order.entities.CartItem;
import com.app.core.APIException;
import com.app.core.ResourceNotFoundException;
import com.app.cart.payloads.CartDTO;
import com.app.product.payloads.ProductDTO;
import com.app.order.repositories.CartItemRepo;
import com.app.order.repositories.CartRepo;

import com.app.commerce.pricing.OrderTotalService;
import com.app.inventory.InventoryReservationService;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

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

	@Override
	@Transactional
	public CartDTO addProductToCart(Long cartId, Long productId, String itemCode, Integer quantity) {

		var cart = cartRepo.findById(cartId)
				.orElseThrow(() -> new ResourceNotFoundException("Cart", "cartId", cartId));

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

		// To be safe, reload or ensure consistency for calculation.
		cart = cartRepo.findById(cartId).orElse(cart);

		var summary = orderTotalService.calculate(cart);
		cart.setTotalPrice(summary.getFinalTotal().doubleValue());
		cartRepo.save(cart);

		// var cartDTO = cartMapper.cartToCartDTO(cart);
		// populateProductDetails(cartDTO, cart);

		var products = getCartProducts(cart);
		return new CartDTO(cart.getCartId(), cart.getTotalPrice(), products);
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
	public CartDTO getCart(String emailId, Long cartId) {
		var cart = cartRepo.findCartByEmailAndCartId(emailId, cartId);
		if (cart == null)
			throw new ResourceNotFoundException("Cart", "cartId", cartId);

		// Optional: Recalculate on View to ensure freshness
		var summary = orderTotalService.calculate(cart);
		cart.setTotalPrice(summary.getFinalTotal().doubleValue());

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
				var summary = orderTotalService.calculate(cart);
				cart.setTotalPrice(summary.getFinalTotal().doubleValue());
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
		var summary = orderTotalService.calculate(cart);
		cart.setTotalPrice(summary.getFinalTotal().doubleValue());
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
		var summary = orderTotalService.calculate(cart);
		cart.setTotalPrice(summary.getFinalTotal().doubleValue());
		cartRepo.save(cart);

		return "Product removed from the cart";
	}

	@Override
	@Transactional
	public CartDTO applyCoupon(Long cartId, String couponCode) {
		var cart = cartRepo.findById(cartId)
				.orElseThrow(() -> new ResourceNotFoundException("Cart", "cartId", cartId));

		// 1. Explicit Validation via CartCouponService
		// This will throw APIException if invalid
		var currentSubtotal = cart.getCartItems().stream()
				.mapToDouble(item -> item.getProductPrice() * item.getQuantity())
				.sum();
		cartCouponService.applyCouponToCart(couponCode, currentSubtotal);

		// 2. Persist code
		cart.setCouponCode(couponCode);

		// 3. Recalculate using full pricing pipeline
		var summary = orderTotalService.calculate(cart);
		cart.setTotalPrice(summary.getFinalTotal().doubleValue());
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
		var summary = orderTotalService.calculate(cart);
		cart.setTotalPrice(summary.getFinalTotal().doubleValue());
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

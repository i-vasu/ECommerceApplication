package com.app.cart;

import com.app.order.services.ERPNextService;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.app.cart.mappers.CartMapper;
import com.app.product.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.app.order.entites.Cart;
import com.app.order.entites.CartItem;
import com.app.core.APIException;
import com.app.core.ResourceNotFoundException;
import com.app.order.payloads.CartDTO;
import com.app.product.payloads.ProductDTO;
import com.app.order.repositories.CartItemRepo;
import com.app.order.repositories.CartRepo;

import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
@Service
public class CartServiceImpl implements CartService {

	@Autowired
	private CartRepo cartRepo;

	@Autowired
	private ProductService productService;

	@Autowired
	private CartItemRepo cartItemRepo;

	@Autowired
	private CartMapper cartMapper;

	@Autowired
	private com.app.commerce.pricing.OrderTotalService orderTotalService;

	@Autowired
	private com.app.inventory.InventoryReservationService inventoryReservationService;

	@Override
	@Transactional
	public CartDTO addProductToCart(Long cartId, Long productId, String itemCode, Integer quantity) {

		Cart cart = cartRepo.findById(cartId)
				.orElseThrow(() -> new ResourceNotFoundException("Cart", "cartId", cartId));

		ProductDTO product = productService.getProductById(productId);
		if (product == null)
			throw new ResourceNotFoundException("Product", "productId", productId);

		String effectiveItemCode = (itemCode != null && !itemCode.isEmpty()) ? itemCode : product.getItemCode();

		CartItem cartItem = cartItemRepo.findCartItemByProductIdAndCartId(cartId, productId);
		// If we support multiple variants of same product in cart, we should check by
		// productId + itemCode.
		// For PoC, let's just check if this specific itemCode + productId + cartId
		// exists.
		// Actually the current repo only has findCartItemByProductIdAndCartId.

		if (cartItem != null && cartItem.getItemCode().equals(effectiveItemCode)) {
			throw new APIException("Item with code " + effectiveItemCode + " already exists in the cart");
		}

        // Use Redis Check (Async Write-Behind compatible)
		if (!inventoryReservationService.checkStock(effectiveItemCode, quantity)) {
			throw new APIException("Insufficient stock for " + effectiveItemCode);
		}

		CartItem newCartItem = new CartItem();
		newCartItem.setProductId(product.getProductId());
		newCartItem.setProductName(product.getProductName());
		newCartItem.setItemCode(effectiveItemCode);
		newCartItem.setCart(cart);
		newCartItem.setQuantity(quantity);
		newCartItem.setDiscount(product.getDiscount());
		newCartItem.setProductPrice(product.getSpecialPrice());

		cartItemRepo.save(newCartItem);

        // Recalculate using Pipeline
        // Need to add item to list first for calculation, but it is saved above.
        // We need to fetch cart again or just trust JPA update?
        // Let's rely on JPA to refresh or manual add for transient calc.
        // cart.getCartItems().add(newCartItem); // Already linked via JPA save?
        // To be safe, reload or ensure consistency.
        
        com.app.commerce.pricing.contracts.OrderSummary summary = orderTotalService.calculate(cart);
		cart.setTotalPrice(summary.getFinalTotal().doubleValue());
		cartRepo.save(cart);

		CartDTO cartDTO = cartMapper.cartToCartDTO(cart);
		populateProductDetails(cartDTO, cart);

		return cartDTO;
	}

	@Override
	public org.springframework.data.domain.Page<CartDTO> getAllCarts(org.springframework.data.domain.Pageable pageable) {
		org.springframework.data.domain.Page<Cart> carts = cartRepo.findAll(pageable);
		return carts.map(cart -> {
			CartDTO dto = cartMapper.cartToCartDTO(cart);
			populateProductDetails(dto, cart);
			return dto;
		});
	}

	@Override
	public CartDTO getCart(String emailId, Long cartId) {
		Cart cart = cartRepo.findCartByEmailAndCartId(emailId, cartId);
		if (cart == null)
			throw new ResourceNotFoundException("Cart", "cartId", cartId);
		
        // Optional: Recalculate on View to ensure freshness
        com.app.commerce.pricing.contracts.OrderSummary summary = orderTotalService.calculate(cart);
		cart.setTotalPrice(summary.getFinalTotal().doubleValue());
        // Don't save on read, just show? Or save to keep sync?
        // For now, simple return.
        
		CartDTO cartDTO = cartMapper.cartToCartDTO(cart);
		populateProductDetails(cartDTO, cart);
		return cartDTO;
	}

	@Override
	@Transactional
	public void updateProductInCarts(Long cartId, Long productId) {
		// Logic to update price if product changed globally
		CartItem cartItem = cartItemRepo.findCartItemByProductIdAndCartId(cartId, productId);
		if (cartItem != null) {
			ProductDTO product = productService.getProductById(productId);
			if (product != null) {
				cartItem.setProductPrice(product.getSpecialPrice());
				cartItemRepo.save(cartItem);
                
                // Recalculate Cart Total
                Cart cart = cartItem.getCart();
                com.app.commerce.pricing.contracts.OrderSummary summary = orderTotalService.calculate(cart);
		        cart.setTotalPrice(summary.getFinalTotal().doubleValue());
                cartRepo.save(cart);
			}
		}
	}

	@Override
	@Transactional
	public CartDTO updateProductQuantityInCart(Long cartId, Long productId, String itemCode, Integer quantity) {
		Cart cart = cartRepo.findById(cartId)
				.orElseThrow(() -> new ResourceNotFoundException("Cart", "cartId", cartId));

		CartItem cartItem = cartItemRepo.findCartItemByProductIdAndCartId(cartId, productId);
		if (cartItem == null)
			throw new ResourceNotFoundException("CartItem", "productId", productId);

		String effectiveItemCode = (itemCode != null && !itemCode.isEmpty()) ? itemCode : cartItem.getItemCode();

		if (!inventoryReservationService.checkStock(effectiveItemCode, quantity)) {
			throw new APIException("Insufficient stock for " + effectiveItemCode);
		}

		cartItem.setQuantity(quantity);
		cartItem.setItemCode(effectiveItemCode);
		cartItemRepo.save(cartItem);

        // Recalculate Pipeline
        com.app.commerce.pricing.contracts.OrderSummary summary = orderTotalService.calculate(cart);
		cart.setTotalPrice(summary.getFinalTotal().doubleValue());
		cartRepo.save(cart);

		CartDTO cartDTO = cartMapper.cartToCartDTO(cart);
		populateProductDetails(cartDTO, cart);
		return cartDTO;
	}

	@Override
	@Transactional
	public String deleteProductFromCart(Long cartId, Long productId) {
		Cart cart = cartRepo.findById(cartId)
				.orElseThrow(() -> new ResourceNotFoundException("Cart", "cartId", cartId));
		CartItem cartItem = cartItemRepo.findCartItemByProductIdAndCartId(cartId, productId);
		if (cartItem == null)
			throw new ResourceNotFoundException("Product", "productId", productId);

		cartItemRepo.delete(cartItem);
        
        // Remove from list for calculation accuracy if not automatically synched
        cart.getCartItems().remove(cartItem); 
        
        // Recalculate Pipeline
        com.app.commerce.pricing.contracts.OrderSummary summary = orderTotalService.calculate(cart);
		cart.setTotalPrice(summary.getFinalTotal().doubleValue());
		cartRepo.save(cart);

		return "Product removed from the cart";
	}

	private void populateProductDetails(CartDTO cartDTO, Cart cart) {
		List<ProductDTO> products = cart.getCartItems().stream()
				.map(p -> productService.getProductById(p.getProductId()))
				.collect(Collectors.toList());
		cartDTO.setProducts(products);
	}
}

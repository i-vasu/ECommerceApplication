package com.app.cart;

import com.app.services.ERPNextService;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.app.entites.Cart;
import com.app.entites.CartItem;
import com.app.exceptions.APIException;
import com.app.exceptions.ResourceNotFoundException;
import com.app.payloads.CartDTO;
import com.app.payloads.ProductDTO;
import com.app.repositories.CartItemRepo;
import com.app.repositories.CartRepo;

import jakarta.transaction.Transactional;

@Transactional
@Service
public class CartServiceImpl implements CartService {

	@Autowired
	private CartRepo cartRepo;

	@Autowired
	private com.app.external.ProductClient productClient;

	@Autowired
	private CartItemRepo cartItemRepo;

	@Autowired
	private ModelMapper modelMapper;

	@Autowired
	private ERPNextService erpNextService;

	@Override
	public CartDTO addProductToCart(Long cartId, Long productId, String itemCode, Integer quantity) {

		Cart cart = cartRepo.findById(cartId)
				.orElseThrow(() -> new ResourceNotFoundException("Cart", "cartId", cartId));

		ProductDTO product = productClient.getProduct(productId);
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

		if (!erpNextService.checkStock(effectiveItemCode, quantity)) {
			throw new APIException("Insufficient stock in ERPNext for " + effectiveItemCode);
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

		cart.setTotalPrice(cart.getTotalPrice() + (product.getSpecialPrice() * quantity));
		cartRepo.save(cart);

		CartDTO cartDTO = modelMapper.map(cart, CartDTO.class);
		populateProductDetails(cartDTO, cart);

		return cartDTO;
	}

	@Override
	public List<CartDTO> getAllCarts() {
		List<Cart> carts = cartRepo.findAll();
		return carts.stream().map(cart -> {
			CartDTO dto = modelMapper.map(cart, CartDTO.class);
			populateProductDetails(dto, cart);
			return dto;
		}).collect(Collectors.toList());
	}

	@Override
	public CartDTO getCart(String emailId, Long cartId) {
		Cart cart = cartRepo.findCartByEmailAndCartId(emailId, cartId);
		if (cart == null)
			throw new ResourceNotFoundException("Cart", "cartId", cartId);
		CartDTO cartDTO = modelMapper.map(cart, CartDTO.class);
		populateProductDetails(cartDTO, cart);
		return cartDTO;
	}

	@Override
	public void updateProductInCarts(Long cartId, Long productId) {
		// Logic to update price if product changed globally
		CartItem cartItem = cartItemRepo.findCartItemByProductIdAndCartId(cartId, productId);
		if (cartItem != null) {
			ProductDTO product = productClient.getProduct(productId);
			if (product != null) {
				cartItem.setProductPrice(product.getSpecialPrice());
				cartItemRepo.save(cartItem);
			}
		}
	}

	@Override
	public CartDTO updateProductQuantityInCart(Long cartId, Long productId, String itemCode, Integer quantity) {
		Cart cart = cartRepo.findById(cartId)
				.orElseThrow(() -> new ResourceNotFoundException("Cart", "cartId", cartId));

		CartItem cartItem = cartItemRepo.findCartItemByProductIdAndCartId(cartId, productId);
		if (cartItem == null)
			throw new ResourceNotFoundException("CartItem", "productId", productId);

		String effectiveItemCode = (itemCode != null && !itemCode.isEmpty()) ? itemCode : cartItem.getItemCode();

		if (!erpNextService.checkStock(effectiveItemCode, quantity)) {
			throw new APIException("Insufficient stock in ERPNext for " + effectiveItemCode);
		}

		double oldPrice = cartItem.getProductPrice() * cartItem.getQuantity();
		cartItem.setQuantity(quantity);
		cartItem.setItemCode(effectiveItemCode);
		cartItemRepo.save(cartItem);

		cart.setTotalPrice(cart.getTotalPrice() - oldPrice + (cartItem.getProductPrice() * quantity));
		cartRepo.save(cart);

		CartDTO cartDTO = modelMapper.map(cart, CartDTO.class);
		populateProductDetails(cartDTO, cart);
		return cartDTO;
	}

	@Override
	public String deleteProductFromCart(Long cartId, Long productId) {
		Cart cart = cartRepo.findById(cartId)
				.orElseThrow(() -> new ResourceNotFoundException("Cart", "cartId", cartId));
		CartItem cartItem = cartItemRepo.findCartItemByProductIdAndCartId(cartId, productId);
		if (cartItem == null)
			throw new ResourceNotFoundException("Product", "productId", productId);

		cart.setTotalPrice(cart.getTotalPrice() - (cartItem.getProductPrice() * cartItem.getQuantity()));
		cartItemRepo.delete(cartItem);
		cartRepo.save(cart);

		return "Product removed from the cart";
	}

	private void populateProductDetails(CartDTO cartDTO, Cart cart) {
		List<ProductDTO> products = cart.getCartItems().stream()
				.map(p -> productClient.getProduct(p.getProductId()))
				.collect(Collectors.toList());
		cartDTO.setProducts(products);
	}
}

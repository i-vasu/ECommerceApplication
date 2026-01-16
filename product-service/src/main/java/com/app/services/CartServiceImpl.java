package com.app.services;

import java.util.List;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.app.entites.Cart;
import com.app.entites.CartItem;
import com.app.entites.Product;
import com.app.exceptions.APIException;
import com.app.exceptions.ResourceNotFoundException;
import com.app.payloads.CartDTO;
import com.app.payloads.ProductDTO;
import com.app.repositories.CartRepo;
import com.app.repositories.ProductRepo;

import jakarta.transaction.Transactional;

@Transactional
@Service
public class CartServiceImpl implements CartService {

	@Autowired
	private CartRepo cartRepo;

	@Autowired
	private ProductRepo productRepo;

	@Autowired
	private ModelMapper modelMapper;

	@Override
	public CartDTO addProductToCart(Long cartId, Long productId, Integer quantity) {
		// Implementation depends on how cart and product service are separated.
		// Since this is product-service, it seems odd to have CartServiceImpl here if CartRepo is also here.
		// Assuming for now simple implementation or stub if logic belongs to order-service.
		// However, CartRepo is present in product-service (as seen in file list earlier), so some cart logic is here.

		// If product-service is responsible for managing products in carts locally (redundancy?):
		Cart cart = cartRepo.findById(cartId)
				.orElseThrow(() -> new ResourceNotFoundException("Cart", "cartId", cartId));

		Product product = productRepo.findById(productId)
				.orElseThrow(() -> new ResourceNotFoundException("Product", "productId", productId));

		CartItem cartItem = cart.getCartItems().stream()
				.filter(item -> item.getProductId().equals(productId))
				.findFirst()
				.orElse(new CartItem());

		if (cartItem.getCartItemId() == null) {
			cartItem.setCart(cart);
			cartItem.setProductId(productId);
			cartItem.setProductName(product.getProductName());
			cartItem.setQuantity(quantity);
			cartItem.setDiscount(product.getDiscount());
			cartItem.setProductPrice(product.getSpecialPrice());
			cart.getCartItems().add(cartItem);
		} else {
			cartItem.setQuantity(cartItem.getQuantity() + quantity);
		}

		cart.setTotalPrice(cart.getTotalPrice() + (product.getSpecialPrice() * quantity));

		cartRepo.save(cart);

		return modelMapper.map(cart, CartDTO.class);
	}

	@Override
	public List<CartDTO> getAllCarts() {
		List<Cart> carts = cartRepo.findAll();

		if (carts.size() == 0) {
			throw new APIException("No cart exists");
		}

		return carts.stream().map(cart -> {
			CartDTO cartDTO = modelMapper.map(cart, CartDTO.class);

			// Map products
			// This logic seems duplicate of what was in ProductServiceImpl updateProduct...
			// But for now just returning mapped DTO
			return cartDTO;
		}).toList();
	}

	@Override
	public CartDTO getCart(String emailId, Long cartId) {
		Cart cart = cartRepo.findCartByEmailAndCartId(emailId, cartId);
		if (cart == null) {
			throw new ResourceNotFoundException("Cart", "cartId", cartId);
		}
		return modelMapper.map(cart, CartDTO.class);
	}

	@Override
	public CartDTO updateProductQuantityInCart(Long cartId, Long productId, Integer quantity) {
		Cart cart = cartRepo.findById(cartId)
				.orElseThrow(() -> new ResourceNotFoundException("Cart", "cartId", cartId));

		Product product = productRepo.findById(productId)
				.orElseThrow(() -> new ResourceNotFoundException("Product", "productId", productId));

		CartItem cartItem = cart.getCartItems().stream()
				.filter(item -> item.getProductId().equals(productId))
				.findFirst()
				.orElseThrow(() -> new APIException("Product " + product.getProductName() + " not available in the cart!!!"));

		double oldPrice = cartItem.getProductPrice() * cartItem.getQuantity();

		cartItem.setQuantity(cartItem.getQuantity() + quantity);

		double newPrice = cartItem.getProductPrice() * cartItem.getQuantity();

		cart.setTotalPrice(cart.getTotalPrice() - oldPrice + newPrice);

		cartRepo.save(cart);

		return modelMapper.map(cart, CartDTO.class);
	}

	@Override
	public void updateProductInCarts(Long cartId, Long productId) {
		Cart cart = cartRepo.findById(cartId)
				.orElseThrow(() -> new ResourceNotFoundException("Cart", "cartId", cartId));

		Product product = productRepo.findById(productId)
				.orElseThrow(() -> new ResourceNotFoundException("Product", "productId", productId));

		CartItem cartItem = cart.getCartItems().stream()
				.filter(item -> item.getProductId().equals(productId))
				.findFirst()
				.orElseThrow(() -> new APIException("Product " + product.getProductName() + " not available in the cart!!!"));

		double oldPrice = cartItem.getProductPrice() * cartItem.getQuantity();

		cartItem.setProductName(product.getProductName());
		cartItem.setProductPrice(product.getSpecialPrice());
		cartItem.setDiscount(product.getDiscount());

		double newPrice = cartItem.getProductPrice() * cartItem.getQuantity();

		cart.setTotalPrice(cart.getTotalPrice() - oldPrice + newPrice);

		cartRepo.save(cart);
	}

	@Override
	public String deleteProductFromCart(Long cartId, Long productId) {
		Cart cart = cartRepo.findById(cartId)
				.orElseThrow(() -> new ResourceNotFoundException("Cart", "cartId", cartId));

		CartItem cartItem = cart.getCartItems().stream()
				.filter(item -> item.getProductId().equals(productId))
				.findFirst()
				.orElseThrow(() -> new ResourceNotFoundException("Product", "productId", productId));

		cart.setTotalPrice(cart.getTotalPrice() - (cartItem.getProductPrice() * cartItem.getQuantity()));

		cart.getCartItems().remove(cartItem);

		cartRepo.save(cart);

		return "Product removed from cart";
	}

}

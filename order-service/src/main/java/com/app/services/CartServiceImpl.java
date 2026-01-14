package com.app.services;

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

	@Override
	public CartDTO addProductToCart(Long cartId, Long productId, Integer quantity) {

		Cart cart = cartRepo.findById(cartId)
				.orElseThrow(() -> new ResourceNotFoundException("Cart", "cartId", cartId));

		ProductDTO product = productClient.getProduct(productId);
		if (product == null)
			throw new ResourceNotFoundException("Product", "productId", productId);

		CartItem cartItem = cartItemRepo.findCartItemByProductIdAndCartId(cartId, productId);

		if (cartItem != null) {
			throw new APIException("Product " + product.getProductName() + " already exists in the cart");
		}

		if (product.getQuantity() == 0) {
			throw new APIException(product.getProductName() + " is not available");
		}

		if (product.getQuantity() < quantity) {
			throw new APIException("Please, make an order of the " + product.getProductName()
					+ " less than or equal to the quantity " + product.getQuantity() + ".");
		}

		CartItem newCartItem = new CartItem();

		newCartItem.setProductId(product.getProductId());
		newCartItem.setProductName(product.getProductName());
		newCartItem.setCart(cart);
		newCartItem.setQuantity(quantity);
		newCartItem.setDiscount(product.getDiscount());
		newCartItem.setProductPrice(product.getSpecialPrice());

		cartItemRepo.save(newCartItem);

		// NOTE: We cannot update remote product quantity directly here.
		// In real microservice, we would reserve stock via API or Event.
		// product.setQuantity(product.getQuantity() - quantity);

		cart.setTotalPrice(cart.getTotalPrice() + (product.getSpecialPrice() * quantity));

		CartDTO cartDTO = modelMapper.map(cart, CartDTO.class);

		// Fetch products again for response? Or just use what we have.
		// For simplicity, we are returning CartDTO which likely expects ProductDTO
		// list.
		// We need to reconstruct it from CartItems + ProductClient calls eventually.
		// For now, let's skip populate full ProductDTOs details in list if avoiding
		// n+1, or fetch them.

		List<ProductDTO> productDTOs = cart.getCartItems().stream()
				.map(p -> {
					ProductDTO dto = productClient.getProduct(p.getProductId());
					return dto;
				}).collect(Collectors.toList());

		cartDTO.setProducts(productDTOs);

		return cartDTO;

	}

	@Override
	public List<CartDTO> getAllCarts() {
		List<Cart> carts = cartRepo.findAll();

		if (carts.size() == 0) {
			throw new APIException("No cart exists");
		}

		List<Long> allProductIds = carts.stream()
				.flatMap(cart -> cart.getCartItems().stream())
				.map(CartItem::getProductId)
				.distinct()
				.collect(Collectors.toList());

		List<ProductDTO> allProducts = productClient.getProductsBatch(allProductIds);
		Map<Long, ProductDTO> productMap = allProducts.stream()
				.collect(Collectors.toMap(ProductDTO::getProductId, p -> p));

		List<CartDTO> cartDTOs = carts.stream().map(cart -> {
			CartDTO cartDTO = modelMapper.map(cart, CartDTO.class);

			List<ProductDTO> products = cart.getCartItems().stream()
					.map(p -> productMap.get(p.getProductId()))
					.filter(p -> p != null)
					.collect(Collectors.toList());

			cartDTO.setProducts(products);

			return cartDTO;

		}).collect(Collectors.toList());

		return cartDTOs;
	}

	@Override
	public CartDTO getCart(String emailId, Long cartId) {
		Cart cart = cartRepo.findCartByEmailAndCartId(emailId, cartId);

		if (cart == null) {
			throw new ResourceNotFoundException("Cart", "cartId", cartId);
		}

		CartDTO cartDTO = modelMapper.map(cart, CartDTO.class);

		List<ProductDTO> products = cart.getCartItems().stream()
				.map(p -> productClient.getProduct(p.getProductId())).collect(Collectors.toList());

		cartDTO.setProducts(products);

		return cartDTO;
	}

	@Override
	public void updateProductInCarts(Long cartId, Long productId) {
		Cart cart = cartRepo.findById(cartId)
				.orElseThrow(() -> new ResourceNotFoundException("Cart", "cartId", cartId));

		ProductDTO product = productClient.getProduct(productId);
		if (product == null)
			throw new ResourceNotFoundException("Product", "productId", productId);

		CartItem cartItem = cartItemRepo.findCartItemByProductIdAndCartId(cartId, productId);

		if (cartItem == null) {
			throw new APIException("Product " + product.getProductName() + " not available in the cart!!!");
		}

		double cartPrice = cart.getTotalPrice() - (cartItem.getProductPrice() * cartItem.getQuantity());

		cartItem.setProductPrice(product.getSpecialPrice());

		cart.setTotalPrice(cartPrice + (cartItem.getProductPrice() * cartItem.getQuantity()));

		cartItem = cartItemRepo.save(cartItem);
	}

	@Override
	public CartDTO updateProductQuantityInCart(Long cartId, Long productId, Integer quantity) {
		Cart cart = cartRepo.findById(cartId)
				.orElseThrow(() -> new ResourceNotFoundException("Cart", "cartId", cartId));

		ProductDTO product = productClient.getProduct(productId);
		if (product == null)
			throw new ResourceNotFoundException("Product", "productId", productId);

		if (product.getQuantity() == 0) {
			throw new APIException(product.getProductName() + " is not available");
		}

		if (product.getQuantity() < quantity) {
			throw new APIException("Please, make an order of the " + product.getProductName()
					+ " less than or equal to the quantity " + product.getQuantity() + ".");
		}

		CartItem cartItem = cartItemRepo.findCartItemByProductIdAndCartId(cartId, productId);

		if (cartItem == null) {
			throw new APIException("Product " + product.getProductName() + " not available in the cart!!!");
		}

		double cartPrice = cart.getTotalPrice() - (cartItem.getProductPrice() * cartItem.getQuantity());

		// Skipping remote stock update for now
		// product.setQuantity(product.getQuantity() + cartItem.getQuantity() -
		// quantity);

		cartItem.setProductPrice(product.getSpecialPrice());
		cartItem.setQuantity(quantity);
		cartItem.setDiscount(product.getDiscount());

		cart.setTotalPrice(cartPrice + (cartItem.getProductPrice() * quantity));

		cartItem = cartItemRepo.save(cartItem);

		CartDTO cartDTO = modelMapper.map(cart, CartDTO.class);

		List<ProductDTO> productDTOs = cart.getCartItems().stream()
				.map(p -> productClient.getProduct(p.getProductId())).collect(Collectors.toList());

		cartDTO.setProducts(productDTOs);

		return cartDTO;

	}

	@Override
	public String deleteProductFromCart(Long cartId, Long productId) {
		Cart cart = cartRepo.findById(cartId)
				.orElseThrow(() -> new ResourceNotFoundException("Cart", "cartId", cartId));

		CartItem cartItem = cartItemRepo.findCartItemByProductIdAndCartId(cartId, productId);

		if (cartItem == null) {
			throw new ResourceNotFoundException("Product", "productId", productId);
		}

		cart.setTotalPrice(cart.getTotalPrice() - (cartItem.getProductPrice() * cartItem.getQuantity()));

		// Skipping remote stock update
		// Product product = cartItem.getProduct();
		// product.setQuantity(product.getQuantity() + cartItem.getQuantity());

		cartItemRepo.deleteCartItemByProductIdAndCartId(cartId, productId);

		return "Product " + cartItem.getProductName() + " removed from the cart !!!";
	}

}

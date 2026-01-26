package com.app.test.integration;

import com.app.cart.CartService;
import com.app.cart.payloads.CartDTO;
import com.app.commerce.checkout.CheckoutService;
import com.app.discount.entities.Coupon;
import com.app.discount.repositories.CouponRepo;
import com.app.identity.entities.User;
import com.app.identity.repositories.UserRepo;
import com.app.order.entities.Cart;
import com.app.order.entities.Order;
import com.app.order.order.OrderService;
import com.app.order.payloads.OrderDTO;
import com.app.order.repositories.CartRepo;
import com.app.order.repositories.OrderRepo;
import com.app.product.entities.Product;
import com.app.product.repositories.ProductRepo;
import com.app.test.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
public class CouponIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private CouponRepo couponRepo;

    @Autowired
    private UserRepo userRepo;

    @Autowired
    private ProductRepo productRepo;

    @Autowired
    private CartRepo cartRepo;

    @Autowired
    private OrderRepo orderRepo;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Test
    void testCouponApplicationAndCheckout() {
        // 1. Setup User and Product via Repos (Pre-requisites)
        User user = new User();
        user.setFirstName("Jonathan");
        user.setEmail("john@example.com");
        user = userRepo.save(user);

        Product product = new Product();
        product.setProductName("Laptop");
        product.setPrice(1000.0);
        product.setQuantity(10);
        product.setDescription("Powerful laptop");
        product.setItemCode("LAP-001");
        product = productRepo.save(product);

        // Pre-populate Redis with Stock to avoid ERPNext call (which fails without
        // Docker)
        redisTemplate.opsForValue().set("inventory:stock:LAP-001", "10");

        // 2. Setup Cart via Repo
        Cart cart = new Cart();
        cart.setUser(user);
        cart = cartRepo.save(cart);

        // 3. Setup Coupon via Repo
        Coupon coupon = new Coupon();
        coupon.setCode("SAVE10");
        coupon.setDiscountType(Coupon.DiscountType.PERCENTAGE);
        coupon.setDiscountValue(10.0);
        coupon.setMinOrderAmount(500.0);
        coupon.setValidFrom(LocalDateTime.now().minusDays(1));
        coupon.setValidTo(LocalDateTime.now().plusDays(30));
        coupon.setActive(true);
        couponRepo.save(coupon);

        // 4. Add Product to Cart via API
        restTestClient.post()
                .uri("/api/v1/public/carts/{cartId}/products/{productId}/quantity/{quantity}",
                        cart.getCartId(), product.getProductId(), 1)
                .exchange()
                .expectStatus().isCreated();

        // 5. Apply Coupon via API
        CartDTO updatedCartDTO = restTestClient.post()
                .uri("/api/v1/public/carts/{cartId}/coupon/{couponCode}",
                        cart.getCartId(), "SAVE10")
                .exchange()
                .expectStatus().isOk()
                .expectBody(CartDTO.class)
                .returnResult().getResponseBody();

        // Verify Cart Total (1000 - 10% = 900)
        assertThat(updatedCartDTO).isNotNull();
        assertThat(updatedCartDTO.totalPrice()).isEqualTo(900.0);

        // 6. Checkout via API
        restTestClient.post()
                .uri("/api/v1/public/users/{emailId}/carts/{cartId}/payments/{paymentMethod}/order",
                        user.getEmail(), cart.getCartId(), "Credit Card")
                .exchange()
                .expectStatus().isCreated();

        // 7. Verify Order via Repo (Final verification)
        Order order = orderRepo.findAllByEmail(user.getEmail()).get(0);
        assertThat(order.getTotalAmount()).isEqualTo(900.0);
        assertThat(order.getCouponCode()).isEqualTo("SAVE10");
        assertThat(order.getDiscountAmount()).isEqualTo(100.0);
    }
}

package com.app.test.integration;

import com.app.cart.entities.Cart;
import com.app.cart.repositories.CartRepo;
import com.app.security.entities.User;
import com.app.security.repositories.UserRepo;
import com.app.test.AbstractIntegrationTest;
import com.app.test.config.TestRestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Import(TestRestConfig.class)
public class CartRestIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private RestTestClient restTestClient;

    @Autowired
    private CartRepo cartRepo;

    @Autowired
    private UserRepo userRepo;

    @Test
    @Transactional
    void testGetCartRest() {
        // Given: A user and a cart exist
        String email = "rest_cart@example.com";
        User user = new User();
        user.setEmail(email);
        user = userRepo.save(user);

        Cart cart = new Cart();
        cart.setUserId(user.getUserId());
        cart.setTotalPrice(BigDecimal.valueOf(100.0));
        cart.setCartItems(new ArrayList<>());
        cart = cartRepo.save(cart);

        // When/Then: Retrieve via REST
        restTestClient.get()
                .uri("/api/public/users/" + email + "/carts/" + cart.getCartId())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.cartId").isEqualTo(cart.getCartId())
                .jsonPath("$.totalPrice").isEqualTo(100.0);
    }
}

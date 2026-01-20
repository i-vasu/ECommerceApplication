package com.app.commerce.pricing.modules;

import com.app.commerce.pricing.OrderTotalModule;
import com.app.commerce.pricing.contracts.OrderSummary;
import com.app.commerce.pricing.contracts.OrderTotal;
import com.app.commerce.pricing.contracts.OrderTotalInput;
import com.app.order.repositories.CartRepo;
import com.app.order.entites.Cart;
import com.app.order.entites.CartItem;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class SubTotalModule implements OrderTotalModule {

    private final CartRepo cartRepo;

    @Override
    public String getName() {
        return "subtotal";
    }

    @Override
    public int getSortOrder() {
        return 10; // Runs First
    }

    @Override
    public OrderTotal calculate(OrderSummary summary, OrderTotalInput input) {
        Cart cart = cartRepo.findById(input.getId()).orElse(null);
        if (cart == null) return null;

        BigDecimal subTotal = BigDecimal.ZERO;
        for (CartItem item : cart.getCartItems()) {
            BigDecimal price = BigDecimal.valueOf(item.getProductPrice());
            BigDecimal qty = BigDecimal.valueOf(item.getQuantity());
            subTotal = subTotal.add(price.multiply(qty));
        }

        return OrderTotal.builder()
                .code("subtotal")
                .title("Sub Total")
                .value(subTotal)
                .sortOrder(10)
                .build();
    }
}

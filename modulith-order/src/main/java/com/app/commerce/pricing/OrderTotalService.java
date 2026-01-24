package com.app.commerce.pricing;

import com.app.commerce.pricing.contracts.OrderSummary;
import com.app.commerce.pricing.contracts.OrderTotal;
import com.app.commerce.pricing.contracts.OrderTotalInput;
import com.app.order.entities.Cart;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderTotalService {

    private final List<OrderTotalModule> modules;

    public OrderSummary calculate(Cart cart) {
        // 1. Prepare Input
        OrderTotalInput input = OrderTotalInput.builder()
                .id(cart.getCartId())
                .email(cart.getUser() != null ? cart.getUser().getEmail() : "guest")
                .couponCode(cart.getCouponCode())
                // In a full implementation, we would resolve addressId to country/state/zip
                .currencyCode("INR")
                .build();

        // 2. Init Summary
        OrderSummary summary = new OrderSummary();

        // 3. Execute Modules in Order
        modules.stream()
                .sorted(Comparator.comparingInt(OrderTotalModule::getSortOrder))
                .forEach(module -> {
                    try {
                        OrderTotal total = module.calculate(summary, input);
                        if (total != null) {
                            summary.addTotal(total);
                        }
                    } catch (Exception e) {
                        log.error("Error in pricing module {}: {}", module.getName(), e.getMessage());
                        // Depending on policy, we might ignore or throw
                    }
                });

        return summary;
    }
}

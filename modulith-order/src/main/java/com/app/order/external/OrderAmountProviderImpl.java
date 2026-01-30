package com.app.order.external;

import com.app.core.contracts.OrderAmountProvider;
import com.app.order.repositories.OrderRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OrderAmountProviderImpl implements OrderAmountProvider {

    private final OrderRepo orderRepo;

    @Override
    public Optional<OrderSummary> getOrderSummary(Long orderId) {
        return orderRepo.findById(orderId)
                .map(order -> new OrderSummary(
                        order.getOrderId(),
                        order.getEmail(),
                        order.getTotalAmount(),
                        order.getOrderStatus().name()));
    }
}

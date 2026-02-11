package com.app.order.external;

import com.app.core.contracts.OrderAmountProvider;
import com.app.order.entities.OrderItem;
import com.app.order.entities.OrderItemTaxDetail;
import com.app.order.repositories.OrderRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderAmountProviderImpl implements OrderAmountProvider {

    private final OrderRepo orderRepo;

    @Override
    @Transactional(readOnly = true)
    public Optional<OrderSummary> getOrderSummary(Long orderId) {
        return orderRepo.findById(orderId)
                .map(order -> {
                    // 1. Calculate Tax Breakdown (CGST, SGST, IGST)
                    Map<String, BigDecimal> taxBreakdown = new HashMap<>();
                    for (OrderItem item : order.getOrderItems()) {
                        for (OrderItemTaxDetail tax : item.getTaxDetails()) {
                            taxBreakdown.merge(tax.getTaxName(), 
                                    tax.getTaxAmount(), 
                                    BigDecimal::add);
                        }
                    }

                    // 2. Map Items
                    List<OrderItemSummary> items = order.getOrderItems().stream()
                            .map(item -> new OrderItemSummary(
                                    item.getItemCode(),
                                    item.getProductName(),
                                    item.getQuantity(),
                                    item.getOrderedPrice(),
                                    item.getTaxDetails().stream()
                                        .map(OrderItemTaxDetail::getTaxAmount)
                                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                                    ))
                            .collect(Collectors.toList());

                    return new OrderSummary(
                            order.getOrderId(),
                            order.getEmail(),
                            order.getTotalAmount(),
                            order.getOrderStatus().name(),
                            order.getTotalTax(),
                            taxBreakdown,
                            items);
                });
    }
}

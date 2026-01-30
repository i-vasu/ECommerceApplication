package com.app.finance.pricing;

import com.app.finance.pricing.contracts.OrderSummary;
import com.app.finance.pricing.contracts.OrderTotal;
import com.app.finance.pricing.contracts.OrderTotalInput;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
public class OrderTotalService {

    private static final Logger log = LoggerFactory.getLogger(OrderTotalService.class);

    private final List<OrderTotalModule> modules;

    public OrderTotalService(List<OrderTotalModule> modules) {
        this.modules = modules;
    }

    public OrderSummary calculate(OrderTotalInput input) {
        // 1. Init Summary
        OrderSummary summary = new OrderSummary();

        // 2. Execute Modules in Order
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
                    }
                });

        return summary;
    }
}

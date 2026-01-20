package com.app.commerce.pricing.contracts;

import lombok.Data;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class OrderSummary {
    private List<OrderTotal> totals = new ArrayList<>();
    private BigDecimal finalTotal = BigDecimal.ZERO;
    
    public void addTotal(OrderTotal total) {
        this.totals.add(total);
        if (total.getValue() != null) {
            this.finalTotal = this.finalTotal.add(total.getValue());
        }
    }
}

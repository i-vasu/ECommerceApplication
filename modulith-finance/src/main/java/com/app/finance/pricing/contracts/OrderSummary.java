package com.app.finance.pricing.contracts;

import lombok.Data;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class OrderSummary {
    private List<OrderTotal> totals = new ArrayList<>();
    private BigDecimal finalTotal = BigDecimal.ZERO;

    public void addTotal(OrderTotal total) {
        this.totals.add(total);
        if (total.getValue() != null) {
            this.finalTotal = this.finalTotal.add(total.getValue());
        }
    }

    public List<OrderTotal> getTotals() {
        return totals;
    }

    public void setTotals(List<OrderTotal> totals) {
        this.totals = totals;
    }

    public BigDecimal getFinalTotal() {
        return finalTotal;
    }

    public void setFinalTotal(BigDecimal finalTotal) {
        this.finalTotal = finalTotal;
    }

    public double getTotal() {
        return finalTotal.doubleValue();
    }

    public double getSubTotal() {
        return totals.stream()
                .filter(total -> "subtotal".equals(total.getCode()))
                .filter(total -> total.getValue() != null)
                .map(total -> total.getValue().doubleValue())
                .findFirst()
                .orElse(0.0);
    }

    public double getDiscountTotal() {
        return totals.stream()
                .filter(total -> "discount".equals(total.getCode()))
                .filter(total -> total.getValue() != null)
                .map(total -> total.getValue().doubleValue())
                .findFirst()
                .orElse(0.0);
    }
}

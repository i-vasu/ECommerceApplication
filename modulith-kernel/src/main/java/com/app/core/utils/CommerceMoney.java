package com.app.core.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;

/**
 * Enterprise Monetary Utility.
 * Replaces 'double' for financial operations, matching Broadleaf's 'Money'
 * logic.
 * Ensures consistent rounding and precision across the platform.
 */
public record CommerceMoney(BigDecimal amount, Currency currency) {

    public static CommerceMoney of(double amount) {
        return new CommerceMoney(BigDecimal.valueOf(amount).setScale(2, RoundingMode.HALF_UP),
                Currency.getInstance("INR"));
    }

    public static CommerceMoney of(BigDecimal amount) {
        return new CommerceMoney(amount.setScale(2, RoundingMode.HALF_UP), Currency.getInstance("INR"));
    }

    public CommerceMoney add(CommerceMoney other) {
        return new CommerceMoney(this.amount.add(other.amount), this.currency);
    }

    public CommerceMoney subtract(CommerceMoney other) {
        return new CommerceMoney(this.amount.subtract(other.amount), this.currency);
    }

    public CommerceMoney multiply(double factor) {
        return new CommerceMoney(this.amount.multiply(BigDecimal.valueOf(factor)).setScale(2, RoundingMode.HALF_UP),
                this.currency);
    }
}

package com.app.core.context;

import com.app.core.utils.CommerceMoney;
import lombok.Builder;
import lombok.Data;
import java.util.Currency;
import java.util.Locale;

/**
 * Enterprise Pricing Context.
 * Mirrors Broadleaf's ability to vary pricing logic based on
 * Locale, Currency, and Customer Segment.
 */
@Data
@Builder
public class PricingContext {

    private final Locale locale;
    private final Currency currency;
    private final String customerSegment;
    private final boolean taxInclusive;

    public static PricingContext defaultContext() {
        return PricingContext.builder()
                .locale(new Locale("en", "IN"))
                .currency(Currency.getInstance("INR"))
                .taxInclusive(true)
                .build();
    }
}

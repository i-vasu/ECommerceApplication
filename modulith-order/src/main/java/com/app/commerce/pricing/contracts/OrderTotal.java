package com.app.commerce.pricing.contracts;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Builder;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderTotal {
    private String code;       // subtotal, tax, shipping, total
    private String title;      // "Sub Total", "GST (18%)"
    private BigDecimal value;  // 100.00
    private int sortOrder;     // 10, 20, 30
    private boolean isPostProcess; // If true, calculated after others
}

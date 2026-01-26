package com.app.commerce.promotion;

import org.springframework.stereotype.Service;
import java.math.BigDecimal;

@Service
public class SimplePromotionService implements PromotionService {

    @Override
    public BigDecimal applyCoupon(String code, BigDecimal subTotal, String email) {
        if (code == null || code.isEmpty()) return BigDecimal.ZERO;
        
        // Mock Implementation for POC
        if ("SAVE10".equalsIgnoreCase(code)) {
            // 10% Off
            return subTotal.multiply(BigDecimal.valueOf(0.10)).negate();
        }
        
        if ("FLAT50".equalsIgnoreCase(code)) {
            return BigDecimal.valueOf(50).negate();
        }

        return BigDecimal.ZERO;
    }
}

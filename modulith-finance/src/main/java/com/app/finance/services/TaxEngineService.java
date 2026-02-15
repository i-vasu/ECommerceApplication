package com.app.finance.services;

import com.app.finance.tax.TaxCalculationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TaxEngineService {

    private final TaxCalculationService taxCalculationService;
    
    // HSN Code Mapping (Simplified for MVP)
    private static final Map<String, BigDecimal> HSN_RATES = new HashMap<>();
    static {
        HSN_RATES.put("6204", BigDecimal.valueOf(0.12)); // Apparel
        HSN_RATES.put("6403", BigDecimal.valueOf(0.18)); // Footwear > 1000
    }

    /**
     * Calculates total tax including GST and TCS (Tax Collected at Source).
     * TCS is applicable for e-commerce operators (e.g., 1%).
     */
    public TaxCalculationResult calculateTotalTax(BigDecimal netTaxableValue, String hsnCode, String state) {
        BigDecimal gstRate = HSN_RATES.getOrDefault(hsnCode, BigDecimal.valueOf(0.18));
        
        // GST Calculation
        TaxCalculationService.TaxCalculation gstResult = taxCalculationService.calculateGST(netTaxableValue, state);
        
        // TCS Calculation (e.g., 1% on net taxable value as per Sec 52 of CGST Act)
        BigDecimal tcsAmount = netTaxableValue.multiply(BigDecimal.valueOf(0.01)).setScale(2, RoundingMode.HALF_UP);
        
        return new TaxCalculationResult(
            gstResult.totalAmount(),
            tcsAmount,
            gstResult.totalAmount().add(tcsAmount),
            gstResult.components()
        );
    }

    public record TaxCalculationResult(
        BigDecimal gstTotal,
        BigDecimal tcsAmount,
        BigDecimal totalTax,
        java.util.List<TaxCalculationService.TaxComponent> gstComponents
    ) {}
}

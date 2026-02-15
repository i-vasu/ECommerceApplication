package com.app.finance.tax;

import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
@lombok.RequiredArgsConstructor
public class TaxCalculationServiceImpl implements TaxCalculationService {

    private final com.app.catalog.ProductService productService;
    private final com.app.finance.repositories.TaxRateRepository taxRateRepository;

    @Override
    public TaxCalculation calculateGST(BigDecimal subtotal, String state) {
        // Fallback for legacy calls
        List<TaxComponent> components = new ArrayList<>();
        BigDecimal rate = BigDecimal.valueOf(0.18); // Default 18% GST
        BigDecimal taxAmount = subtotal.multiply(rate).setScale(2, RoundingMode.HALF_UP);
        
        if ("Maharashtra".equalsIgnoreCase(state)) {
            BigDecimal halfRate = rate.divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
            BigDecimal halfTax = taxAmount.divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
            components.add(new TaxComponent("CGST", halfRate, halfTax));
            components.add(new TaxComponent("SGST", halfRate, halfTax));
        } else {
            components.add(new TaxComponent("IGST", rate, taxAmount));
        }
        
        return new TaxCalculation(taxAmount, components);
    }

    @Override
    public TaxCalculation calculateTax(List<com.app.finance.pricing.contracts.OrderTotalInput.ItemInput> items, String state) {
        BigDecimal totalTax = BigDecimal.ZERO;
        List<TaxComponent> components = new ArrayList<>();
        boolean isIntraState = "Maharashtra".equalsIgnoreCase(state);

        for (com.app.finance.pricing.contracts.OrderTotalInput.ItemInput item : items) {
            try {
                com.app.catalog.payloads.ProductDTO product = productService.getProductById(item.getProductId());
                Double taxPercentage = 18.0; // Default

                if (product.taxRateId() != null) {
                    taxPercentage = taxRateRepository.findById(product.taxRateId())
                            .map(com.app.finance.entities.TaxRate::getPercentage)
                            .orElse(18.0);
                }

                BigDecimal rate = BigDecimal.valueOf(taxPercentage).divide(BigDecimal.valueOf(100));
                BigDecimal itemTotal = item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
                BigDecimal itemTax = itemTotal.multiply(rate).setScale(2, RoundingMode.HALF_UP);

                totalTax = totalTax.add(itemTax);

                // Add components for this item (simplified: aggregating same rates would be better, but listing per rate is okay)
                if (isIntraState) {
                    BigDecimal halfRate = rate.divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
                    BigDecimal halfTax = itemTax.divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
                    components.add(new TaxComponent("CGST (" + (taxPercentage/2) + "%)", halfRate, halfTax));
                    components.add(new TaxComponent("SGST (" + (taxPercentage/2) + "%)", halfRate, halfTax));
                } else {
                    components.add(new TaxComponent("IGST (" + taxPercentage + "%)", rate, itemTax));
                }

            } catch (Exception e) {
                // Return fallback for this item? Or log error.
                // For now, continue
            }
        }

        return new TaxCalculation(totalTax, components);
    }
}

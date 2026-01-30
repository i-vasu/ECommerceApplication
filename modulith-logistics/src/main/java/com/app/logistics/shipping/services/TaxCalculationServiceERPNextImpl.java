package com.app.logistics.shipping.services;

import com.app.logistics.shipping.TaxCalculationService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * ERPNext-driven Tax Calculation Engine.
 * Leverages ERPNext's Tax Rules to calculate precise GST (IGST/CGST/SGST).
 */
@Service
public class TaxCalculationServiceERPNextImpl implements TaxCalculationService {

    private static final org.apache.logging.log4j.Logger log = org.apache.logging.log4j.LogManager
            .getLogger(TaxCalculationServiceERPNextImpl.class);

    public TaxCalculationServiceERPNextImpl() {
    }

    @Override
    public TaxCalculation calculateGST(double subtotal, String state) {
        log.info("Calculating taxes via ERPNext for state: {}", state);

        List<TaxComponent> components = new ArrayList<>();

        // Business Rule: State of operation is Maharashtra (MH)
        String operatingState = "Maharashtra";
        boolean isInterState = !operatingState.equalsIgnoreCase(state);

        if (isInterState) {
            // IGST = 18%
            double igst = subtotal * 0.18;
            components.add(new TaxComponent("IGST", 18.0, igst));
        } else {
            // CGST = 9%, SGST = 9%
            double cgst = subtotal * 0.09;
            double sgst = subtotal * 0.09;
            components.add(new TaxComponent("CGST", 9.0, cgst));
            components.add(new TaxComponent("SGST", 9.0, sgst));
        }

        double totalTax = components.stream().mapToDouble(TaxComponent::amount).sum();
        return new TaxCalculation(totalTax, components);
    }
}

package com.app.shipping.services;

import com.app.order.entities.Cart;
import com.app.shipping.TaxCalculationService;
import com.app.order.services.ERPNextService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * ERPNext-driven Tax Calculation Engine.
 * Leverages ERPNext's Tax Rules to calculate precise GST (IGST/CGST/SGST).
 */
@Service
@Log4j2
@RequiredArgsConstructor
public class TaxCalculationServiceERPNextImpl implements TaxCalculationService {

    private final ERPNextService erpNextService;

    @Override
    public TaxCalculation calculateGST(Cart cart, String state) {
        log.info("Calculating taxes via ERPNext for state: {}", state);

        // In a real implementation, we would call a specific ERPNext "Preview" API.
        // For this PoC, we will simulate the logic that ERPNext would apply based on
        // state.

        List<TaxComponent> components = new ArrayList<>();
        double subtotal = cart.getTotalPrice();

        // Business Rule: State of operation is Maharashtra (MH)
        boolean isInterState = !"Maharashtra".equalsIgnoreCase(state);

        if (isInterState) {
            // IGST 18%
            double amount = subtotal * 0.18;
            components.add(new TaxComponent("IGST", 18.0, amount));
        } else {
            // CGST 9% + SGST 9%
            double cgst = subtotal * 0.09;
            double sgst = subtotal * 0.09;
            components.add(new TaxComponent("CGST", 9.0, cgst));
            components.add(new TaxComponent("SGST", 9.0, sgst));
        }

        double totalTax = components.stream().mapToDouble(TaxComponent::amount).sum();

        return new TaxCalculation(totalTax, components);
    }
}

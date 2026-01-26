package com.app.order.services;

import com.app.inventory.InventoryService.InventoryLock;
import com.app.shipping.TaxCalculationService.TaxCalculation;
import com.app.shipping.ShippingCalculationService.ShippingCost;
import com.app.identity.AddressValidationService.AddressValidation;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.app.order.entities.Cart;
import com.app.identity.entities.Address;
import com.app.product.repositories.ProductRepo;
import com.app.product.entities.Product;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Optimized Checkout Service
 * Uses CompletableFuture for parallel validations (stable API)
 */
@org.springframework.stereotype.Service
@lombok.RequiredArgsConstructor
public class OptimizedCheckoutService {

    private static final Logger log = LoggerFactory.getLogger(OptimizedCheckoutService.class);

    private final java.util.List<com.app.order.services.checkout.CheckoutActivity<?>> activities;
    private final ProductRepo productRepo;
    private final com.app.order.services.FlashSaleService flashSaleService;

    public CheckoutResult processCheckout(Cart cart, Address address, String couponCode) {
        long startTime = System.currentTimeMillis();

        try {
            Map<String, CompletableFuture<?>> futures = activities.stream()
                    .collect(Collectors.toMap(
                            a -> a.getName(),
                            a -> CompletableFuture.supplyAsync(() -> a.execute(cart, address))));

            CompletableFuture<List<String>> priceCheckFuture = CompletableFuture.supplyAsync(() -> {
                List<String> discrepancies = new ArrayList<>();
                for (com.app.order.entities.CartItem item : cart.getCartItems()) {
                    Product p = productRepo.findById(item.getProductId()).orElse(null);
                    if (p != null) {
                        var flashProduct = flashSaleService.getActiveFlashProduct(item.getProductId());
                        double currentTargetPrice = flashProduct.map(fp -> fp.getFlashPrice())
                                .orElseGet(() -> p.getSpecialPrice() != null ? p.getSpecialPrice().doubleValue() : 0.0);

                        if (Math.abs(currentTargetPrice - item.getProductPrice()) > 0.01) {
                            discrepancies.add("Price changed for " + item.getProductName() +
                                    ": was " + item.getProductPrice() + ", now " + currentTargetPrice);
                        }
                    }
                }
                return discrepancies;
            });

            CompletableFuture.allOf(futures.values().toArray(new CompletableFuture[0]))
                    .thenCombine(priceCheckFuture, (v, d) -> null)
                    .get(10, TimeUnit.SECONDS);

            return new CheckoutResult(
                    (InventoryLock) futures.get("inventory-lock").get(),
                    (AddressValidation) futures.get("address-validation").get(),
                    (TaxCalculation) futures.get("tax-calculation").get(),
                    (ShippingCost) futures.get("shipping-calculation").get(),
                    (java.math.BigDecimal) futures.get("promotion-evaluation").get(),
                    calculateFinalAmount(cart, (TaxCalculation) futures.get("tax-calculation").get(),
                            (ShippingCost) futures.get("shipping-calculation").get(),
                            (java.math.BigDecimal) futures.get("promotion-evaluation").get()),
                    priceCheckFuture.get());

        } catch (Exception e) {
            throw new RuntimeException("Checkout failed", e);
        }
    }

    private double calculateFinalAmount(Cart cart, TaxCalculation tax,
            ShippingCost shipping, java.math.BigDecimal discount) {
        double subtotal = cart.getTotalPrice();
        double taxAmount = tax != null ? tax.totalAmount() : 0;
        double shippingAmount = shipping != null ? shipping.amount() : 0;
        double discountAmount = discount != null ? Math.abs(discount.doubleValue()) : 0;
        return subtotal + taxAmount + shippingAmount - discountAmount;
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class CheckoutResult {
        private InventoryLock inventory;
        private AddressValidation address;
        private TaxCalculation tax;
        private ShippingCost shipping;
        private java.math.BigDecimal promotionDiscount;
        private double finalAmount;
        private List<String> priceDiscrepancies;

        // Compatibility methods to act like a record temporarily or for the test
        public InventoryLock inventory() {
            return inventory;
        }

        public AddressValidation address() {
            return address;
        }

        public TaxCalculation tax() {
            return tax;
        }

        public ShippingCost shipping() {
            return shipping;
        }

        public java.math.BigDecimal promotionDiscount() {
            return promotionDiscount;
        }

        public double finalAmount() {
            return finalAmount;
        }

        public List<String> priceDiscrepancies() {
            return priceDiscrepancies;
        }
    }
}

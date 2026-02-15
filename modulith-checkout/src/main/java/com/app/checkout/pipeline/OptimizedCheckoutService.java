package com.app.checkout.pipeline;

import com.app.catalog.entities.Product;
import com.app.catalog.repositories.ProductRepo;
import com.app.checkout.domain.FlashSaleService;
import com.app.checkout.domain.FraudDetectionService;
import com.app.checkout.domain.PriceGuardService;
import com.app.core.APIException;
import com.app.core.contracts.CartContract;
import com.app.governance.rules.RuleEngineService;
import com.app.governance.states.OperationalStateMachineService;
import com.app.governance.states.OrderEvent;
import com.app.logistics.inventory.InventoryService.InventoryLock;
import com.app.logistics.shipping.ShippingCalculationService.ShippingCost;
import com.app.finance.tax.TaxCalculationService.TaxCalculation;
import com.app.security.AddressValidationService.AddressValidation;
import com.app.security.entities.Address;
import com.app.logistics.inventory.InventoryReservationService;
import io.micrometer.context.ContextExecutorService;
import io.micrometer.context.ContextSnapshot;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.annotation.Observed;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class OptimizedCheckoutService {

    private static final Logger log = LoggerFactory.getLogger(OptimizedCheckoutService.class);

    private final java.util.List<CheckoutActivity<?>> activities;
    private final ProductRepo productRepo;
    private final com.app.security.repositories.UserRepo userRepo;
    private final FlashSaleService flashSaleService;
    private final FraudDetectionService fraudService;
    private final PriceGuardService priceGuard;
    private final MeterRegistry meterRegistry;
    private final ObservationRegistry observationRegistry;
    private final RuleEngineService ruleEngine;
    private final OperationalStateMachineService stateMachineService;
    private final InventoryReservationService inventoryReservationService;

    private ExecutorService tracingExecutor;
    private Counter checkoutAttempts;
    private DistributionSummary checkoutRevenue;

    public OptimizedCheckoutService(
            java.util.List<CheckoutActivity<?>> activities,
            ProductRepo productRepo,
            com.app.security.repositories.UserRepo userRepo,
            FlashSaleService flashSaleService,
            FraudDetectionService fraudService,
            PriceGuardService priceGuard,
            MeterRegistry meterRegistry,
            ObservationRegistry observationRegistry,
            RuleEngineService ruleEngine,
            OperationalStateMachineService stateMachineService,
            InventoryReservationService inventoryReservationService) {
        this.activities = activities;
        this.productRepo = productRepo;
        this.userRepo = userRepo;
        this.flashSaleService = flashSaleService;
        this.fraudService = fraudService;
        this.priceGuard = priceGuard;
        this.meterRegistry = meterRegistry;
        this.observationRegistry = observationRegistry;
        this.ruleEngine = ruleEngine;
        this.stateMachineService = stateMachineService;
        this.inventoryReservationService = inventoryReservationService;
    }

    @PostConstruct
    public void init() {
        this.tracingExecutor = ContextExecutorService.wrap(
                Executors.newFixedThreadPool(10),
                () -> ContextSnapshot.captureAll(observationRegistry));

        this.checkoutAttempts = meterRegistry.counter("checkout.attempts");
        this.checkoutRevenue = meterRegistry.summary("checkout.revenue");
    }

    /**
     * Non-committing checkout summary.
     * Skips inventory locking and state machine transitions.
     */
    public CheckoutResult getSummary(CartContract cart, Address address) {
        log.info("Generating checkout summary for cart: {}", cart.cartId());
        
        // 1. Parallel Step Execution (Filtering out inventory-lock)
        Map<String, CompletableFuture<?>> futures = activities.stream()
                .filter(a -> !a.getName().equals("inventory-lock"))
                .collect(Collectors.toMap(
                        activity -> activity.getName(),
                        activity -> CompletableFuture.supplyAsync(() -> activity.execute(cart, address), (java.util.concurrent.Executor) tracingExecutor)));

        try {
            CompletableFuture.allOf(futures.values().toArray(new CompletableFuture[0]))
                    .get(5, TimeUnit.SECONDS);

            return new CheckoutResult(
                    null, // No lock for summary
                    (AddressValidation) futures.get("address-validation").get(),
                    (TaxCalculation) futures.get("tax-calculation").get(),
                    (ShippingCost) futures.get("shipping-calculation").get(),
                    (java.math.BigDecimal) futures.get("promotion-evaluation").get(),
                    calculateFinalAmount(cart, (TaxCalculation) futures.get("tax-calculation").get(),
                            (ShippingCost) futures.get("shipping-calculation").get(),
                            (java.math.BigDecimal) futures.get("promotion-evaluation").get()),
                    List.of()); // No price guard check for summary

        } catch (Exception e) {
            log.error("Checkout summary generation failed: {}", e.getMessage());
            throw new RuntimeException("Failed to generate checkout summary", e);
        }
    }

    @Observed(name = "checkout.process", contextualName = "optimized-checkout-pipeline")
    public CheckoutResult processCheckout(CartContract cart, Address address) {
        log.info("Starting optimized checkout for cart: {}", cart.cartId());

        // 1. Autonomous Fraud Check
        com.app.security.entities.User user = userRepo.findById(cart.userId()).orElse(null);
        if (fraudService.isFraudulent(cart, user)) {
            log.warn("Fraud detected for user {}! Blocking checkout.",
                    (user != null) ? user.getEmail() : "unknown");
            throw new APIException(
                    "Security violation: Checkout blocked. Our risk engine detected suspicious activity.");
        }

        // 2. Dynamic SpEL Policy Check
        Map<String, Object> context = new java.util.HashMap<>();
        context.put("cart", cart);
        context.put("user", user);

        // Rule: Unverified users cannot check out > 10,000 INR
        String checkoutRule = "(!user.verified && cart.subTotal < 10000) || user.verified";

        if (!ruleEngine.evaluate(checkoutRule, context)) {
            log.warn("Checkout policy violation for user {}",
                    (user != null) ? user.getEmail() : "unknown");
            throw new APIException(
                    "Policy Violation: Unverified accounts are limited to ₹10,000 per transaction. Please verify your email.");
        }

        // Formalize the beginning of the journey in State Machine
        stateMachineService.triggerOrderEvent(0L, OrderEvent.PLACE);

        checkoutAttempts.increment();

        try {
            // 2. Parallel Step Execution
            Map<String, CompletableFuture<?>> futures = activities.stream()
                    .collect(Collectors.toMap(
                            activity -> activity.getName(),
                            activity -> CompletableFuture.supplyAsync(() -> activity.execute(cart, address), (java.util.concurrent.Executor) tracingExecutor)));

            // 2.5 Batch Fetch Products for Price Guard & Validation Efficiency
            List<Long> productIds = cart.items().stream().map(CartContract.CartItemContract::productId).toList();
            Map<Long, Product> productMap = productRepo.findAllById(productIds).stream()
                    .collect(Collectors.toMap(Product::getProductId, p -> p));

            CompletableFuture<List<String>> priceCheckFuture = CompletableFuture.supplyAsync(() -> {
                List<String> discrepancies = new ArrayList<>();
                for (CartContract.CartItemContract item : cart.items()) {
                    Product p = productMap.get(item.productId());
                    if (p != null) {
                        var flashProduct = flashSaleService.getActiveFlashProduct(item.productId());
                        java.util.Optional<com.app.finance.promo.entities.FlashSaleProduct> fpOpt = flashProduct;
                        java.math.BigDecimal currentTargetPrice = fpOpt.map(fp -> fp.getFlashPrice())
                                .orElseGet(() -> p.getSpecialPrice() != null ? p.getSpecialPrice()
                                        : p.getPrice());

                        if (currentTargetPrice.subtract(item.price()).abs().compareTo(java.math.BigDecimal.valueOf(0.01)) > 0) {
                            discrepancies.add("Price changed for " + item.productName());
                        }
                    }
                }
                return discrepancies;
            }, tracingExecutor);

            InventoryLock lock = null;
            try {
                CompletableFuture.allOf(futures.values().toArray(new CompletableFuture[0]))
                        .thenCombine(priceCheckFuture, (v, d) -> null)
                        .get(10, TimeUnit.SECONDS);

                lock = (InventoryLock) futures.get("inventory-lock").get();

                CheckoutResult result = new CheckoutResult(
                        lock,
                        (AddressValidation) futures.get("address-validation").get(),
                        (TaxCalculation) futures.get("tax-calculation").get(),
                        (ShippingCost) futures.get("shipping-calculation").get(),
                        (java.math.BigDecimal) futures.get("promotion-evaluation").get(),
                        calculateFinalAmount(cart, (TaxCalculation) futures.get("tax-calculation").get(),
                                (ShippingCost) futures.get("shipping-calculation").get(),
                                (java.math.BigDecimal) futures.get("promotion-evaluation").get()),
                        priceCheckFuture.get());

                // 3. Last-Mile Price Guard (Margin Protection)
                if (!priceGuard.isPriceSafe(cart.items())) {
                    throw new APIException(
                            "Financial Safeguard: An item in your cart has an invalid price configuration. Please try again later.");
                }

                checkoutRevenue.record(result.finalAmount().doubleValue());
                return result;

            } catch (Exception e) {
                log.error("Checkout execution pipeline failed. Releasing inventory if locked.");
                // Release stock if it was successfully locked before the pipeline failed
                if (lock == null) {
                    try {
                        var lockFuture = futures.get("inventory-lock");
                        if (lockFuture != null && lockFuture.isDone() && !lockFuture.isCompletedExceptionally()) {
                            lock = (InventoryLock) lockFuture.get();
                        }
                    } catch (Exception ignored) {}
                }

                if (lock != null && lock.locked()) {
                   log.warn("Releasing stale inventory reservation {} due to pipeline failure: {}", lock.lockId(), e.getMessage());
                   cart.items().forEach(item -> {
                       try {
                           // Default to W1/B1 as per Activity logic
                           inventoryReservationService.releaseStock(item.itemCode(), item.quantity());
                       } catch (Exception ex) {
                           log.error("Failed to release item {} after checkout failure", item.itemCode());
                       }
                   });
                }
                throw e;
            }

        } catch (Exception e) {
            log.error("Checkout execution failed: {}", e.getMessage());
            throw new RuntimeException("Checkout failed: " + e.getMessage(), e);
        }
    }

    private java.math.BigDecimal calculateFinalAmount(CartContract cart, TaxCalculation tax,
            ShippingCost shipping, java.math.BigDecimal discount) {
        java.math.BigDecimal subtotal = cart.subTotal();
        java.math.BigDecimal taxAmount = tax != null ? tax.totalAmount()
                : java.math.BigDecimal.ZERO;
        java.math.BigDecimal shippingAmount = shipping != null ? shipping.amount()
                : java.math.BigDecimal.ZERO;
        java.math.BigDecimal discountAmount = discount != null ? discount.abs() : java.math.BigDecimal.ZERO;
        return subtotal.add(taxAmount).add(shippingAmount).subtract(discountAmount);
    }

    public record CheckoutResult(
            InventoryLock inventory,
            AddressValidation address,
            TaxCalculation tax,
            ShippingCost shipping,
            java.math.BigDecimal promotionDiscount,
            java.math.BigDecimal finalAmount,
            List<String> priceDiscrepancies) {
    }
}

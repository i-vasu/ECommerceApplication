package com.app.checkout.pipeline;

import com.app.cart.entities.Cart;
import com.app.catalog.entities.Product;
import com.app.catalog.repositories.ProductRepo;
import com.app.checkout.domain.FlashSaleService;
import com.app.checkout.domain.FraudDetectionService;
import com.app.checkout.domain.PriceGuardService;
import com.app.core.APIException;
import com.app.governance.rules.RuleEngineService;
import com.app.governance.states.OperationalStateMachineService;
import com.app.governance.states.OrderEvent;
import com.app.logistics.inventory.InventoryService.InventoryLock;
import com.app.logistics.shipping.ShippingCalculationService.ShippingCost;
import com.app.logistics.shipping.TaxCalculationService.TaxCalculation;
import com.app.security.AddressValidationService.AddressValidation;
import com.app.security.entities.Address;
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
            OperationalStateMachineService stateMachineService) {
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
    }

    @PostConstruct
    public void init() {
        this.tracingExecutor = ContextExecutorService.wrap(
                Executors.newFixedThreadPool(10),
                () -> ContextSnapshot.captureAll(observationRegistry));

        this.checkoutAttempts = Counter.builder("checkout.attempts")
                .description("Total number of checkout attempts")
                .register(meterRegistry);

        this.checkoutRevenue = DistributionSummary.builder("checkout.revenue")
                .description("Total revenue from successful checkouts")
                .baseUnit("currency")
                .register(meterRegistry);
    }

    @Observed(name = "checkout.process", contextualName = "optimized-checkout-pipeline")
    public CheckoutResult processCheckout(Cart cart, Address address) {
        log.info("Starting optimized checkout for cart: {}", cart.getCartId());

        // 1. Autonomous Fraud Check
        var user = userRepo.findById(cart.getUserId()).orElse(null);
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
        String checkoutRule = "(!user.verified && cart.totalPrice < 10000) || user.verified";

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
            Map<String, CompletableFuture<?>> futures = activities.stream()
                    .collect(Collectors.toMap(
                            CheckoutActivity::getName,
                            a -> CompletableFuture.supplyAsync(() -> a.execute(cart, address), tracingExecutor)));

            CompletableFuture<List<String>> priceCheckFuture = CompletableFuture.supplyAsync(() -> {
                List<String> discrepancies = new ArrayList<>();
                for (com.app.cart.entities.CartItem item : cart.getCartItems()) {
                    Product p = productRepo.findById(item.getProductId()).orElse(null);
                    if (p != null) {
                        var flashProduct = flashSaleService.getActiveFlashProduct(item.getProductId());
                        double currentTargetPrice = flashProduct.map(fp -> fp.getFlashPrice())
                                .orElseGet(() -> p.getSpecialPrice() != null ? p.getSpecialPrice().doubleValue()
                                        : p.getPrice().doubleValue());

                        if (Math.abs(currentTargetPrice - item.getProductPrice().doubleValue()) > 0.01) {
                            discrepancies.add("Price changed for " + item.getProductName());
                        }
                    }
                }
                return discrepancies;
            }, tracingExecutor);

            CompletableFuture.allOf(futures.values().toArray(new CompletableFuture[0]))
                    .thenCombine(priceCheckFuture, (v, d) -> null)
                    .get(10, TimeUnit.SECONDS);

            CheckoutResult result = new CheckoutResult(
                    (InventoryLock) futures.get("inventory-lock").get(),
                    (AddressValidation) futures.get("address-validation").get(),
                    (TaxCalculation) futures.get("tax-calculation").get(),
                    (ShippingCost) futures.get("shipping-calculation").get(),
                    (java.math.BigDecimal) futures.get("promotion-evaluation").get(),
                    calculateFinalAmount(cart, (TaxCalculation) futures.get("tax-calculation").get(),
                            (ShippingCost) futures.get("shipping-calculation").get(),
                            (java.math.BigDecimal) futures.get("promotion-evaluation").get()),
                    priceCheckFuture.get());

            // 3. Last-Mile Price Guard (Margin Protection)
            if (!priceGuard.isPriceSafe(cart.getCartItems())) {
                throw new APIException(
                        "Financial Safeguard: An item in your cart has an invalid price configuration. Please try again later.");
            }

            checkoutRevenue.record(result.finalAmount().doubleValue());
            return result;

        } catch (Exception e) {
            log.error("Checkout execution failed: {}", e.getMessage());
            throw new RuntimeException("Checkout failed: " + e.getMessage(), e);
        }
    }

    private java.math.BigDecimal calculateFinalAmount(Cart cart, TaxCalculation tax,
            ShippingCost shipping, java.math.BigDecimal discount) {
        java.math.BigDecimal subtotal = cart.getTotalPrice();
        java.math.BigDecimal taxAmount = tax != null ? java.math.BigDecimal.valueOf(tax.totalAmount())
                : java.math.BigDecimal.ZERO;
        java.math.BigDecimal shippingAmount = shipping != null ? java.math.BigDecimal.valueOf(shipping.amount())
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

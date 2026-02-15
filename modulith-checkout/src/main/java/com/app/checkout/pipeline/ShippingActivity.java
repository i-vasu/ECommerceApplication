package com.app.checkout.pipeline;

import com.app.core.contracts.CartContract;
import com.app.logistics.shipping.ShippingCalculationService;
import com.app.logistics.shipping.ShippingCalculationService.ShippingCost;
import com.app.security.entities.Address;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ShippingActivity implements CheckoutActivity<ShippingCost> {

    private final ShippingCalculationService shippingCalculationService;
    private final com.app.catalog.repositories.ProductRepo productRepo;

    @Override
    public String getName() {
        return "shipping-calculation";
    }

    @Override
    public ShippingCost execute(CartContract cart, Address address) {
        String pincode = (address != null) ? address.getPincode() : "000000";
        
        double totalWeight = cart.items().stream()
                .mapToDouble(item -> {
                    var product = productRepo.findById(item.productId()).orElse(null);
                    return (product != null && product.getKgWeight() != null) 
                        ? product.getKgWeight() * item.quantity() 
                        : 0.5 * item.quantity();
                })
                .sum();

        return shippingCalculationService.calculateCost(pincode, totalWeight);
    }
}

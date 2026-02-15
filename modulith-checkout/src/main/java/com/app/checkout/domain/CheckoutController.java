package com.app.checkout.domain;

import com.app.checkout.pipeline.OptimizedCheckoutService;
import com.app.checkout.pipeline.OptimizedCheckoutService.CheckoutResult;
import com.app.core.contracts.CartContract;
import com.app.core.payloads.ApiResponse;
import com.app.core.version.ApiVersion;
import com.app.security.entities.Address;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@ApiVersion(1)
@Tag(name = "Checkout", description = "Operations related to order checkout and cost calculation")
@RequiredArgsConstructor
public class CheckoutController {

    private final OptimizedCheckoutService checkoutService;
    private final com.app.cart.domain.CartService cartService;
    // GAP-18: Fixed Architectural Leak (Controller -> Repository Bypass)
    private final com.app.security.AddressService addressService;

    @Operation(summary = "Get Checkout Summary", description = "Calculates final costs (tax, shipping, discounts) for a cart and address without creating an order or locking inventory.")
    @PostMapping("/public/checkout/summary")
    public ResponseEntity<ApiResponse<CheckoutResult>> getCheckoutSummary(
            @Parameter(description = "ID of the cart") @RequestParam Long cartId,
            @Parameter(description = "ID of the shipping address") @RequestParam Long addressId) {

        // Fetch cart using the correct service method
        com.app.cart.payloads.CartDTO cartDTO = cartService.getCartById(cartId);
        CartContract cartContract = convertToContract(cartDTO);

        // 2. Fetch Address via Service (No direct Repo access)
        com.app.security.payloads.AddressDTO addressDTO = addressService.getAddress(addressId);
        Address address = mapToAddressEntity(addressDTO);

        // 3. Get Summary
        CheckoutResult summary = checkoutService.getSummary(cartContract, address);

        return ResponseEntity.ok(ApiResponse.success(summary, "Checkout summary calculated"));
    }

    private CartContract convertToContract(com.app.cart.payloads.CartDTO dto) {
        return new CartContract(
                dto.cartId(),
                1L, // userId - should be fetched from context
                dto.totalPrice(),
                dto.couponCode(),
                dto.items().stream()
                        .map(i -> new CartContract.CartItemContract(
                                i.productId(),
                                i.itemCode(),
                                i.productName(),
                                i.quantity(),
                                i.productPrice(),
                                i.discount()))
                        .toList()
        );
    }

    // Helper to map DTO back to Entity for the Checkout Pipeline (which requires Entity for now)
    private Address mapToAddressEntity(com.app.security.payloads.AddressDTO dto) {
        Address address = new Address();
        address.setAddressId(dto.addressId());
        address.setStreet(dto.street());
        address.setBuildingName(dto.buildingName());
        address.setCity(dto.city());
        address.setState(dto.state());
        address.setCountry(dto.country());
        address.setPincode(dto.pincode());
        // Map other fields if necessary
        return address;
    }
}

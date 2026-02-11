package com.app.finance.pricing;

import com.app.core.payloads.ApiResponse;
import com.app.core.version.ApiVersion;
import com.app.finance.pricing.contracts.OrderSummary;
import com.app.finance.pricing.contracts.OrderTotalInput;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/pricing")
@ApiVersion(1)
@SecurityRequirement(name = "E-Commerce Application")
@RequiredArgsConstructor
public class PricingController {

    private final OrderTotalService orderTotalService;

    @PostMapping("/calculate")
    public ResponseEntity<ApiResponse<OrderSummary>> calculate(@RequestBody OrderTotalInput input) {
        OrderSummary summary = orderTotalService.calculate(input);
        return ResponseEntity.ok(ApiResponse.success(summary, "Pricing calculated successfully"));
    }
}

package com.app.security.controllers;

import com.app.core.ResourceNotFoundException;
import com.app.security.entities.User;
import com.app.security.entities.WalletTransaction;
import com.app.security.repositories.UserRepo;
import com.app.security.services.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/user/account")
@RequiredArgsConstructor
public class UserAccountController {

    private final WalletService walletService;
    private final UserRepo userRepo;

    @GetMapping("/wallet")
    public ResponseEntity<?> getWalletDetails() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Double balance = walletService.getBalance(email);
        List<WalletTransaction> transactions = walletService.getTransactions(email);

        return ResponseEntity.ok(Map.of(
                "balance", balance,
                "transactions", transactions));
    }

    @GetMapping("/reward-points")
    public ResponseEntity<?> getRewardPoints() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        Integer rewardPoints = 0;
        String customerGroup = "RETAIL";

        if (user.getLoyalty() != null) {
            rewardPoints = user.getLoyalty().getRewardPoints();
            customerGroup = user.getLoyalty().getCustomerGroup();
        }

        return ResponseEntity.ok(Map.of(
                "rewardPoints", rewardPoints,
                "customerGroup", customerGroup));
    }
}

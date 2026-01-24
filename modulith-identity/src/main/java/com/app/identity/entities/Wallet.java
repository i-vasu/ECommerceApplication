package com.app.identity.entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "user_wallets")
@Data
@NoArgsConstructor
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long walletId;

    @OneToOne
    @JoinColumn(name = "user_id", unique = true)
    private User user;

    private Double balance = 0.0;

    @UpdateTimestamp
    private LocalDateTime lastUpdated;

    @OneToMany(mappedBy = "wallet", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WalletTransaction> transactions = new ArrayList<>();
}

package com.app.order.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "order_vendors")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderVendor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String email;
    
    private BigDecimal commissionRate = BigDecimal.valueOf(0.15); // Default 15%
    
    private BigDecimal pendingSettlement = BigDecimal.ZERO;
    
    private String bankAccountNumber;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public BigDecimal getCommissionRate() { return commissionRate; }
    public void setCommissionRate(BigDecimal commissionRate) { this.commissionRate = commissionRate; }

    public BigDecimal getPendingSettlement() { return pendingSettlement; }
    public void setPendingSettlement(BigDecimal pendingSettlement) { this.pendingSettlement = pendingSettlement; }

    public String getBankAccountNumber() { return bankAccountNumber; }
    public void setBankAccountNumber(String bankAccountNumber) { this.bankAccountNumber = bankAccountNumber; }
}

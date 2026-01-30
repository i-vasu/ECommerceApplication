package com.app.order.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Entity
@Table(name = "vendors")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Vendor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String email;
    
    private BigDecimal commissionRate = BigDecimal.valueOf(0.15); // Default 15%
    
    private BigDecimal pendingSettlement = BigDecimal.ZERO;
    
    private String bankAccountNumber;
}

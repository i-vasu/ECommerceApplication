package com.app.finance.entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "tax_rates")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaxRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "zoho_tax_id", unique = true)
    private String zohoTaxId;

    @Column(name = "tax_name")
    private String taxName;

    @Column(name = "percentage")
    private Double percentage;
}

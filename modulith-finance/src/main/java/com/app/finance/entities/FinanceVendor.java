package com.app.finance.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.envers.Audited;

import java.math.BigDecimal;

@Entity
@Table(name = "finance_vendors")
@Audited
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FinanceVendor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(unique = true)
    private String email;

    private String phoneNumber;

    @Column(name = "gstin", length = 15)
    private String gstin; // Indian GST Number

    private String pan; // Indian PAN Number

    @Column(name = "category")
    @Enumerated(EnumType.STRING)
    private VendorCategory category;

    @Embedded
    private Address address;

    @Column(name = "commission_rate", precision = 5, scale = 2)
    private BigDecimal commissionRate = BigDecimal.valueOf(15.00);

    private String bankName;
    private String bankAccountNumber;
    private String ifscCode;

    private String paymentTerms; // e.g., "NET-30", "IMMEDIATE"

    private boolean active = true;

    public enum VendorCategory {
        FABRIC,
        STITCHING,
        FINISHED_GOODS,
        TRIMS,
        LOGISTICS,
        OTHER
    }

    @Embeddable
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Address {
        private String street;
        private String city;
        private String state;
        private String pincode;
        private String country = "India";
    }
}

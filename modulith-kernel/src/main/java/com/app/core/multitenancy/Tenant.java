package com.app.core.multitenancy;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tenants", schema = "public")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Tenant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String tenantId; // e.g., "tenant1_test", "tenant1_prod"

    @Column(nullable = false)
    private String name;

    private String razorpayKeyId;
    private String razorpayKeySecret;
    private String razorpayWebhookSecret;

    private String shippingProvider; // SHIPROCKET, SHADOWFAX
    private String shadowfaxToken;

    @Column(nullable = false)
    private String environment; // TEST, PROD

    private boolean active = true;

    // UI Configuration
    private String logoUrl;
    private String accentColor;
    private String supportEmail;
    private String heroVideoUrl;
    private String heroPosterUrl;

    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private java.util.Map<String, String> socialLinks;

    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private java.util.Map<String, String> staticPages;

    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private java.util.Map<String, Object> footerMenu;
}

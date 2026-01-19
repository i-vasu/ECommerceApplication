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

    private String erpNextUrl;
    private String erpNextApiKey;
    private String erpNextApiSecret;

    @Column(nullable = false)
    private String environment; // TEST, PROD

    private boolean active = true;
}

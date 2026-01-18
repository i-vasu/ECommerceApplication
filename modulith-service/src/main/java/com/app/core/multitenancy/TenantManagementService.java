package com.app.core.multitenancy;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TenantManagementService {

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void init() {
        initializeTenants();
    }

    @Transactional
    public void initializeTenants() {
        String[][] tenants = {
                { "tenant1_test", "Tenant 1 Test", "TEST" },
                { "tenant1_prod", "Tenant 1 Prod", "PROD" },
                { "tenant2_test", "Tenant 2 Test", "TEST" },
                { "tenant2_prod", "Tenant 2 Prod", "PROD" }
        };

        for (String[] t : tenants) {
            String tenantId = t[0];
            String name = t[1];
            String env = t[2];

            if (tenantRepository.findByTenantId(tenantId).isEmpty()) {
                Tenant tenant = new Tenant();
                tenant.setTenantId(tenantId);
                tenant.setName(name);
                tenant.setEnvironment(env);
                // Use site-specific URL mapping
                // Requires 127.0.0.1 tenant1-test.localhost in /etc/hosts
                String siteUrl = "http://" + tenantId.replace("_", "-") + ".localhost:8000";
                tenant.setErpNextUrl(siteUrl);
                tenantRepository.save(tenant);

                // Ensure schema exists in Postgres
                jdbcTemplate.execute("CREATE SCHEMA IF NOT EXISTS " + tenantId);
            }
        }
    }

    public List<Tenant> getActiveTenants() {
        return tenantRepository.findAllByActiveTrue();
    }
}

package com.app.config;

import com.app.security.entities.Role;
import com.app.security.entities.User;
import com.app.security.entities.UserProfile;
import com.app.security.repositories.RoleRepo;
import com.app.security.repositories.UserRepo;
import com.app.catalog.repositories.ProductRepo;
import com.app.catalog.repositories.CategoryRepo;
import com.app.catalog.entities.Product;
import com.app.catalog.entities.Category;
import com.app.core.multitenancy.Tenant;
import com.app.core.multitenancy.TenantRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private RoleRepo roleRepo;

    @Autowired
    private UserRepo userRepo;

    @Autowired
    private ProductRepo productRepo;

    @Autowired
    private CategoryRepo categoryRepo;

    @Autowired
    private TenantRepository tenantRepository;



    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // ... (existing user/role init code remains same, omitted for brevity) ...
        Role adminRole = initRole("ADMIN");
        initRole("USER");

        // Initialize Admin User
        String adminEmail = "admin@vasu.com";
        if (userRepo.findByEmail(adminEmail).isEmpty()) {
            User admin = new User();
            admin.setEmail(adminEmail);
            
            UserProfile profile = new UserProfile();
            profile.setFirstName("AdminUser");
            profile.setLastName("SystemAdmin");
            profile.setMobileNumber("9000000000");
            profile.setUser(admin);
            admin.setProfile(profile);
            
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setVerified(true);
            admin.setRoles(Set.of(adminRole));

            userRepo.save(admin);
            System.out.println(">>> Create A Role: Default Admin User Initialized -> admin@vasu.com / admin123");
        }

        // Initialize ParadeDB Search Index (BM25)
        try {
            System.out.println(">>> Initializing ParadeDB BM25 Index...");
            jdbcTemplate.execute("CALL paradedb.create_bm25(" +
                    "'products_search_idx', " +
                    "'products', " +
                    "'product_id', " +
                    "description => 'description', " +
                    "product_name => 'product_name'" +
                    ")");
            System.out.println(">>> ParadeDB BM25 Index 'products_search_idx' created successfully.");
        } catch (Exception e) {
            System.out.println(">>> ParadeDB Index init note (likely already exists): " + e.getMessage());
        }

        // Initialize Default Tenant
        String defaultTenantId = "vaabhi";
        Tenant tenant = tenantRepository.findByTenantId(defaultTenantId).orElseGet(() -> {
            Tenant t = new Tenant();
            t.setTenantId(defaultTenantId);
            t.setName("VAABHI | Heritage Luxury");
            t.setEnvironment("PRODUCTION");
            t.setActive(true);
            return tenantRepository.save(t);
        });

        // Initialize Catalog Data from Seed if empty
        if (productRepo.count() == 0) {
            System.out.println(">>> Product Catalog is empty. Waiting for SQL seeding to complete...");
            // SQL seeding happens via Flyway/Init script usually, but if we need manual check:
            // The seed_products.sql in postgres-init runs on container creation.
            // If we are restarting backend but DB persists, data might already be there.
        }
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Role initRole(String name) {
        return roleRepo.findByRoleName(name).orElseGet(() -> {
            Role role = new Role();
            role.setRoleName(name);
            return roleRepo.save(role);
        });
    }
}

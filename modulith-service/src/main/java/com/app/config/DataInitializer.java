package com.app.config;

import com.app.security.entities.Role;
import com.app.security.entities.User;
import com.app.security.repositories.RoleRepo;
import com.app.security.repositories.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Set;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private RoleRepo roleRepo;

    @Autowired
    private UserRepo userRepo;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // Initialize Roles if not present
        Role adminRole = initRole("ADMIN");
        initRole("USER");

        // Initialize Admin User
        String adminEmail = "admin@vasu.com";
        if (userRepo.findByEmail(adminEmail).isEmpty()) {
            User admin = new User();
            admin.setEmail(adminEmail);
            admin.setFirstName("AdminUser");
            admin.setLastName("SystemAdmin");
            admin.setMobileNumber("9000000000");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setVerified(true);
            admin.setRoles(Set.of(adminRole));

            userRepo.save(admin);
            System.out.println(">>> Create A Role: Default Admin User Initialized -> admin@vasu.com / admin123");
        }

        // Initialize ParadeDB Search Index (BM25)
        // This offloads search from standard Postgres to the high-performance search
        // engine
        try {
            System.out.println(">>> Initializing ParadeDB BM25 Index...");
            // Check if index exists or just recreate (Idempotent call handles specific
            // logic usually, here we rely on SQL)
            // Note: create_bm25 is usually: CALL paradedb.create_bm25(index_name,
            // table_name, key_field, text_fields...)
            // Syntax: CALL paradedb.create_bm25('products_search_idx', 'products',
            // 'product_id', 'product_name', 'description');

            // We use jdbcTemplate to execute raw SQL since JPA doesn't support CALL
            // natively well for this extension
            jdbcTemplate.execute("CALL paradedb.create_bm25(" +
                    "'products_search_idx', " +
                    "'products', " +
                    "'product_id', " +
                    "description => 'description', " +
                    "product_name => 'product_name'" +
                    ")");
            System.out.println(">>> ParadeDB BM25 Index 'products_search_idx' created successfully.");
        } catch (Exception e) {
            // Ignore if already exists or handle specifically
            System.out.println(">>> ParadeDB Index init note (likely already exists): " + e.getMessage());
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

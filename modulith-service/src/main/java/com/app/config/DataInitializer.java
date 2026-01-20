package com.app.config;

import com.app.identity.entities.Role;
import com.app.identity.entities.User;
import com.app.identity.repositories.RoleRepo;
import com.app.identity.repositories.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
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
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // Initialize Roles if not present
        Role adminRole = initRole("ADMIN");
        Role userRole = initRole("USER");

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
    }
    
    private Role initRole(String name) {
        return roleRepo.findByRoleName(name).orElseGet(() -> {
            Role role = new Role();
            role.setRoleName(name);
            return roleRepo.save(role);
        });
    }
}

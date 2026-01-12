package com.app;

import java.util.List;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;

import com.app.config.AppConstants;
import com.app.entites.Role;
import com.app.repositories.RoleRepo;
import com.app.entites.Category;
import com.app.entites.Product;
import com.app.repositories.CategoryRepo;
import com.app.repositories.ProductRepo;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;

@EnableCaching
@EnableScheduling
@SpringBootApplication
@SecurityScheme(name = "E-Commerce Application", scheme = "bearer", type = SecuritySchemeType.HTTP, in = SecuritySchemeIn.HEADER)
public class ECommerceApplication implements CommandLineRunner {

	@Autowired
	private RoleRepo roleRepo;

	public static void main(String[] args) {
		SpringApplication.run(ECommerceApplication.class, args);
	}

	@Bean
	public ModelMapper modelMapper() {
		return new ModelMapper();
	}

	@Bean
	public RestTemplate restTemplate() {
		return new RestTemplate();
	}

	@Override
	public void run(String... args) throws Exception {
		try {
			Role adminRole = new Role();
			adminRole.setRoleId(AppConstants.ADMIN_ID);
			adminRole.setRoleName("ADMIN");

			Role userRole = new Role();
			userRole.setRoleId(AppConstants.USER_ID);
			userRole.setRoleName("USER");

			Iterable<Role> roles = List.of(adminRole, userRole);

			List<Role> savedRoles = roleRepo.saveAll(roles);

			savedRoles.forEach(System.out::println);

			// Bootstrap items for PoC
			if (roleRepo.count() > 0) {
				Category category = categoryRepo.findByCategoryName("Fashion");
				if (category == null) {
					category = new Category();
					category.setCategoryName("Fashion");
					category = categoryRepo.save(category);
				}

				if (productRepo.count() == 0) {
					Product p1 = new Product();
					p1.setProductName("Classic White T-Shirt");
					p1.setDescription("Premium cotton white t-shirt for daily wear.");
					p1.setPrice(999.0);
					p1.setDiscount(10.0);
					p1.setSpecialPrice(899.0);
					p1.setQuantity(50);
					p1.setCategory(category);
					p1.setImage(
							"https://images.unsplash.com/photo-1521572163474-6864f9cf17ab?auto=format&fit=crop&w=800&q=80");
					productRepo.save(p1);

					Product p2 = new Product();
					p2.setProductName("Blue Denim Jeans");
					p2.setDescription("Classic fit blue denim jeans.");
					p2.setPrice(2499.0);
					p2.setDiscount(20.0);
					p2.setSpecialPrice(1999.0);
					p2.setQuantity(30);
					p2.setCategory(category);
					p2.setImage(
							"https://images.unsplash.com/photo-1542272604-787c3835535d?auto=format&fit=crop&w=800&q=80");
					productRepo.save(p2);

					Product p3 = new Product();
					p3.setProductName("Leather Jacket");
					p3.setDescription("Elegant black leather jacket.");
					p3.setPrice(4999.0);
					p3.setDiscount(15.0);
					p3.setSpecialPrice(4249.0);
					p3.setQuantity(10);
					p3.setCategory(category);
					p3.setImage(
							"https://images.unsplash.com/photo-1551028719-00167b16eac5?auto=format&fit=crop&w=800&q=80");
					productRepo.save(p3);
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Autowired
	private CategoryRepo categoryRepo;

	@Autowired
	private ProductRepo productRepo;
}

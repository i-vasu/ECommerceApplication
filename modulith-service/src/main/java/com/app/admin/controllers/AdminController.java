package com.app.admin.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.ui.Model;
import org.springframework.beans.factory.annotation.Autowired;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @GetMapping("/login")
    public String login() {
        return "admin/login";
    }

    @Autowired
    private com.app.product.ProductService productService;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("pageTitle", "Dashboard");
        return "admin/dashboard";
    }

    @GetMapping("/products")
    public String products(Model model) {
        model.addAttribute("pageTitle", "Products Management");
        try {
            var response = productService.getAllProducts(0, 100, "productName", "asc");
            model.addAttribute("products", response.getContent());
        } catch (Exception e) {
            model.addAttribute("error", "Failed to load products: " + e.getMessage());
        }
        return "admin/products";
    }
}

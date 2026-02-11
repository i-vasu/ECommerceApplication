package com.app.support.admin.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

/**
 * Legacy Admin Controller - Refactored to redirect to Vaadin Native views.
 */
@Controller
@RequestMapping("/admin")
public class AdminController {

    @GetMapping("/login")
    public String login() {
        // Keeping this for Spring Security form login if needed, 
        // Although Vaadin can handle its own login.
        return "admin/login";
    }

    @GetMapping("/dashboard")
    public String dashboard() {
        return "redirect:/admin/analytics-native";
    }

    @GetMapping("/users")
    public String users() {
        return "redirect:/admin/users-native";
    }

    @GetMapping("/products")
    public String products() {
        return "redirect:/admin/products-native";
    }

    @GetMapping("/inventory")
    public String inventory() {
        return "redirect:/admin/inventory-native";
    }

    @GetMapping("/support")
    public String support() {
        return "redirect:/admin/support-native";
    }

    @GetMapping("/orders")
    public String orders() {
        return "redirect:/admin/orders-native";
    }
}

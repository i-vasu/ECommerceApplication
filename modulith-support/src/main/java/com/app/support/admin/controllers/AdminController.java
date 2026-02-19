package com.app.support.admin.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

/**
 * Legacy Admin Controller - Refactored to redirect to Vaadin Native views.
 */
@Controller
public class AdminController {

    @GetMapping("/admin/login")
    public String login() {
        // Keeping this for Spring Security form login if needed, 
        // Although Vaadin can handle its own login.
        return "admin/login";
    }

    @GetMapping("/admin/dashboard")
    public String dashboard() {
        return "redirect:/admin/dashboard-native";
    }

    @GetMapping("/admin/analytics")
    public String analytics() {
        return "redirect:/admin/analytics-native";
    }

    @GetMapping("/admin/users")
    public String users() {
        return "redirect:/admin/users-native";
    }

    @GetMapping("/admin/products")
    public String products() {
        return "redirect:/admin/products-native";
    }

    @GetMapping("/admin/inventory")
    public String inventory() {
        return "redirect:/admin/inventory-native";
    }

    @GetMapping("/admin/support")
    public String support() {
        return "redirect:/admin/support-native";
    }

    @GetMapping("/admin/orders")
    public String orders() {
        return "redirect:/admin/orders-native";
    }

    @GetMapping("/admin/system-health")
    public String systemHealth() {
        return "redirect:/admin/operational-dashboard-native";
    }

    @GetMapping("/admin/tech-monitoring")
    public String techMonitoring() {
        return "redirect:/admin/tech-monitoring-native";
    }

    @GetMapping("/admin/analytics-native-query")
    public String analyticsNative() {
        return "redirect:/admin/analytics-native";
    }
}

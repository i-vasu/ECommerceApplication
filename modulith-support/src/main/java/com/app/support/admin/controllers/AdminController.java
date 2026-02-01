package com.app.support.admin.controllers;

import com.app.support.domain.SupportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @GetMapping("/login")
    public String login() {
        return "admin/login";
    }

    /*
     * @Autowired
     * private ProductService productService;
     */

    @Autowired
    private SupportService supportService;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("pageTitle", "Dashboard");
        return "admin/dashboard";
    }

    /*
     * @GetMapping("/products")
     * public String products(Model model) {
     * model.addAttribute("pageTitle", "Products Management");
     * try {
     * var response = productService.getAllProducts(0, 100, "productName", "asc");
     * model.addAttribute("products", response.content());
     * } catch (Exception e) {
     * model.addAttribute("error", "Failed to load products: " + e.getMessage());
     * }
     * return "admin/products";
     * }
     */

    @GetMapping("/support")
    public String support(Model model, @RequestParam(defaultValue = "0") int page) {
        model.addAttribute("pageTitle", "Support Tickets");
        try {
            var pageable = PageRequest.of(page, 20,
                    Sort.by("createdAt").descending());
            var tickets = supportService.getAllTickets(pageable);
            model.addAttribute("tickets", tickets);
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", tickets.getTotalPages());
        } catch (Exception e) {
            model.addAttribute("error", "Failed to load tickets: " + e.getMessage());
            model.addAttribute("tickets", Page.empty());
            model.addAttribute("currentPage", 0);
            model.addAttribute("totalPages", 0);
        }
        return "admin/support";
    }

    @GetMapping("/support/{id}")
    public String supportDetails(@PathVariable Long id, Model model) {
        model.addAttribute("pageTitle", "Ticket Details");
        try {
            var ticket = supportService.getTicketById(id);
            model.addAttribute("ticket", ticket);
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/admin/support";
        }
        return "admin/support_details";
    }

    @PostMapping("/support/{id}/reply")
    public String replyToTicket(@PathVariable Long id, @RequestParam String message) {
        try {
            supportService.adminReplyToTicket(id, message, "Admin");
        } catch (Exception e) {
            // log error
        }
        return "redirect:/admin/support/" + id;
    }
}

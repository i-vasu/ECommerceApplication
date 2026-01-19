package com.app.admin.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.ui.Model;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @GetMapping("/login")
    public String login() {
        return "admin/login";
    }

    @Autowired
    private com.app.product.ProductService productService;

    @Autowired
    private com.app.customer_service.repositories.SupportTicketRepo ticketRepo;

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

    @GetMapping("/support")
    public String support(Model model, @RequestParam(defaultValue = "0") int page) {
        model.addAttribute("pageTitle", "Support Tickets");
        try {
            var pageable = org.springframework.data.domain.PageRequest.of(page, 20, org.springframework.data.domain.Sort.by("createdAt").descending());
            var tickets = ticketRepo.findAll(pageable);
            model.addAttribute("tickets", tickets);
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", tickets.getTotalPages());
        } catch (Exception e) {
            model.addAttribute("error", "Failed to load tickets: " + e.getMessage());
        }
        return "admin/support";
    }

    @GetMapping("/support/{id}")
    public String supportDetails(@PathVariable Long id, Model model) {
        model.addAttribute("pageTitle", "Ticket Details");
        try {
            var ticket = ticketRepo.findById(id).orElseThrow(() -> new RuntimeException("Ticket not found"));
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
             var ticket = ticketRepo.findById(id).orElseThrow(() -> new RuntimeException("Ticket not found"));
             com.app.order.entites.TicketMessage msg = new com.app.order.entites.TicketMessage();
             msg.setTicket(ticket);
             msg.setSenderType("ADMIN");
             msg.setSenderId("Admin"); // Ideally get from SecurityContext
             msg.setMessage(message);
             msg.setTimestamp(java.time.LocalDateTime.now());
             
             ticket.getMessages().add(msg);
             if (!"CLOSED".equals(ticket.getStatus())) {
                 ticket.setStatus("IN_PROGRESS");
             }
             ticketRepo.save(ticket);
        } catch (Exception e) {
            // log error
        }
        return "redirect:/admin/support/" + id;
    }
}

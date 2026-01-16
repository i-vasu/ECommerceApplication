package com.app.controllers;

import com.app.entites.SupportTicket;
import com.app.repositories.SupportTicketRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/support")
public class SupportController {

    @Autowired
    private SupportTicketRepo ticketRepo;

    @PostMapping("/tickets")
    public ResponseEntity<SupportTicket> createTicket(@RequestBody com.app.payloads.TicketDTO ticketDTO) {
        SupportTicket ticket = new SupportTicket();
        ticket.setUserEmail(ticketDTO.getUserEmail());
        ticket.setSubject(ticketDTO.getSubject());
        ticket.setRelatedOrderId(ticketDTO.getRelatedOrderId());
        ticket.setStatus("OPEN");
        ticket.setCreatedAt(java.time.LocalDateTime.now());

        com.app.entites.TicketMessage message = new com.app.entites.TicketMessage();
        message.setTicket(ticket);
        message.setSenderType("USER");
        message.setSenderId(ticketDTO.getUserEmail());
        message.setMessage(ticketDTO.getMessage());
        message.setTimestamp(java.time.LocalDateTime.now());

        ticket.getMessages().add(message);

        return ResponseEntity.ok(ticketRepo.save(ticket));
    }

    @PostMapping("/tickets/{ticketId}/reply")
    public ResponseEntity<SupportTicket> replyToTicket(@PathVariable Long ticketId,
            @RequestBody com.app.entites.TicketMessage message) {
        SupportTicket ticket = ticketRepo.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));

        message.setTicket(ticket);
        message.setTimestamp(java.time.LocalDateTime.now());

        ticket.getMessages().add(message);
        // Re-open ticket if closed and user replies?
        // Logic: if user reply -> IN_PROGRESS. If admin reply -> RESOLVED?
        // For now, let's keep status manual or update to IN_PROGRESS on reply.
        if (!"CLOSED".equals(ticket.getStatus())) {
            // Optional logic
        }

        return ResponseEntity.ok(ticketRepo.save(ticket));
    }

    @GetMapping("/tickets/{email}")
    public ResponseEntity<List<SupportTicket>> getUserTickets(@PathVariable String email) {
        return ResponseEntity.ok(ticketRepo.findByUserEmail(email));
    }

    @GetMapping("/admin/tickets")
    public ResponseEntity<List<SupportTicket>> getAllTickets() {
        return ResponseEntity.ok(ticketRepo.findAll());
    }

    @PutMapping("/admin/tickets/{ticketId}/status")
    public ResponseEntity<SupportTicket> updateTicketStatus(@PathVariable Long ticketId, @RequestParam String status) {
        SupportTicket ticket = ticketRepo.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));
        ticket.setStatus(status);
        return ResponseEntity.ok(ticketRepo.save(ticket));
    }
}

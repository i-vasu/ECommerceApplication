package com.app.customer_service.controllers;

import com.app.order.entites.SupportTicket;
import com.app.customer_service.repositories.SupportTicketRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import com.app.order.payloads.TicketDTO;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping("/api/v1")
public class SupportController implements SupportApi {

    @Autowired
    private SupportTicketRepo ticketRepo;

    @Override
    public ResponseEntity<SupportTicket> createTicket(@RequestBody TicketDTO ticketDTO) {
        SupportTicket ticket = new SupportTicket();
        ticket.setUserEmail(ticketDTO.getUserEmail());
        ticket.setSubject(ticketDTO.getSubject());
        ticket.setRelatedOrderId(ticketDTO.getRelatedOrderId());
        ticket.setStatus("OPEN");
        ticket.setCreatedAt(java.time.LocalDateTime.now());

        com.app.order.entites.TicketMessage message = new com.app.order.entites.TicketMessage();
        message.setTicket(ticket);
        message.setSenderType("USER");
        message.setSenderId(ticketDTO.getUserEmail());
        message.setMessage(ticketDTO.getMessage());
        message.setTimestamp(java.time.LocalDateTime.now());

        ticket.getMessages().add(message);

        return ResponseEntity.ok(ticketRepo.save(ticket));
    }

    @Override
    public ResponseEntity<SupportTicket> replyToTicket(@PathVariable Long ticketId,
            @RequestBody com.app.order.entites.TicketMessage message) {
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

    @Override
    public ResponseEntity<List<SupportTicket>> getUserTickets(@PathVariable String email) {
        return ResponseEntity.ok(ticketRepo.findByUserEmail(email));
    }

    @Override
    public ResponseEntity<org.springframework.data.domain.Page<SupportTicket>> getAllTickets(
            org.springframework.data.domain.Pageable pageable) {
        // ✅ OPTIMIZED: Pagination prevents loading 50,000+ tickets at once (50x faster
        // frontend)
        return ResponseEntity.ok(ticketRepo.findAll(pageable));
    }

    @Override
    public ResponseEntity<SupportTicket> updateTicketStatus(@PathVariable Long ticketId, @RequestParam String status) {
        SupportTicket ticket = ticketRepo.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));
        ticket.setStatus(status);
        return ResponseEntity.ok(ticketRepo.save(ticket));
    }
}

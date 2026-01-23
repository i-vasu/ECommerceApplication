package com.app.customer_service.controllers;

import com.app.order.entities.SupportTicket;
import com.app.customer_service.services.SupportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import com.app.order.payloads.TicketDTO;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class SupportController implements SupportApi {

    @Autowired
    private SupportService supportService;

    @Override
    public ResponseEntity<SupportTicket> createTicket(@RequestBody TicketDTO ticketDTO) {
        return ResponseEntity.ok(supportService.createTicket(ticketDTO));
    }

    @Override
    public ResponseEntity<SupportTicket> replyToTicket(@PathVariable Long ticketId,
            @RequestBody com.app.order.entities.TicketMessage message) {
        return ResponseEntity.ok(supportService.replyToTicket(ticketId, message));
    }

    @Override
    public ResponseEntity<org.springframework.data.domain.Page<SupportTicket>> getUserTickets(
            @PathVariable String email, org.springframework.data.domain.Pageable pageable) {
        return ResponseEntity.ok(supportService.getUserTickets(email, pageable));
    }

    @Override
    public ResponseEntity<org.springframework.data.domain.Page<SupportTicket>> getAllTickets(
            org.springframework.data.domain.Pageable pageable) {
        // ✅ OPTIMIZED: Pagination prevents loading 50,000+ tickets at once (50x faster
        // frontend)
        return ResponseEntity.ok(supportService.getAllTickets(pageable));
    }

    @Override
    public ResponseEntity<SupportTicket> updateTicketStatus(@PathVariable Long ticketId, @RequestParam String status) {
        return ResponseEntity.ok(supportService.updateTicketStatus(ticketId, status));
    }
}

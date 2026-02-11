package com.app.support.domain;

import com.app.support.entities.SupportTicket;
import com.app.support.entities.TicketMessage;
import com.app.support.payloads.TicketDTO;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tickets")
@SecurityRequirement(name = "E-Commerce Application")
@RequiredArgsConstructor
public class SupportTicketController {

    private final SupportService supportService;

    @PostMapping
    public ResponseEntity<SupportTicket> createTicket(@RequestBody TicketDTO ticketDTO) {
        return new ResponseEntity<>(supportService.createTicket(ticketDTO), HttpStatus.CREATED);
    }

    @GetMapping("/{email}")
    public ResponseEntity<Page<SupportTicket>> getUserTickets(@PathVariable String email, Pageable pageable) {
        return ResponseEntity.ok(supportService.getUserTickets(email, pageable));
    }

    @GetMapping("/detail/{ticketId}")
    public ResponseEntity<SupportTicket> getTicket(@PathVariable Long ticketId) {
        return ResponseEntity.ok(supportService.getTicketById(ticketId));
    }

    @PostMapping("/{ticketId}/reply")
    public ResponseEntity<SupportTicket> replyToTicket(@PathVariable Long ticketId,
            @RequestBody TicketMessage message) {
        return ResponseEntity.ok(supportService.replyToTicket(ticketId, message));
    }
}

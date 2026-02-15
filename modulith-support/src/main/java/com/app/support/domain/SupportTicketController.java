package com.app.support.domain;

import com.app.support.entities.SupportTicket;
import com.app.support.entities.TicketMessage;
import com.app.support.payloads.TicketDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tickets")
@Tag(name = "Support Tickets", description = "Customer Support Ticket Management APIs")
@SecurityRequirement(name = "E-Commerce Application")
@RequiredArgsConstructor
public class SupportTicketController {

    private final SupportService supportService;

    @Operation(summary = "Create Ticket", description = "Opens a new support ticket.")
    @ApiResponse(responseCode = "201", description = "Ticket created successfully")
    @PostMapping
    public ResponseEntity<SupportTicket> createTicket(@RequestBody TicketDTO ticketDTO) {
        return new ResponseEntity<>(supportService.createTicket(ticketDTO), HttpStatus.CREATED);
    }

    @Operation(summary = "List User Tickets", description = "Retrieves all support tickets for a specific user.")
    @GetMapping("/{email}")
    public ResponseEntity<Page<SupportTicket>> getUserTickets(
            @Parameter(description = "Customer email address") @PathVariable String email, Pageable pageable) {
        return ResponseEntity.ok(supportService.getUserTickets(email, pageable));
    }

    @Operation(summary = "Ticket Details", description = "Retrieves the full thread and status of a specific ticket.")
    @GetMapping("/detail/{ticketId}")
    public ResponseEntity<SupportTicket> getTicket(@Parameter(description = "ID of the ticket") @PathVariable Long ticketId) {
        String email = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        boolean isAdmin = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication()
                .getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (isAdmin) {
            return ResponseEntity.ok(supportService.getTicketById(ticketId));
        } else {
            return ResponseEntity.ok(supportService.getTicketForUser(ticketId, email));
        }
    }

    @Operation(summary = "Reply to Ticket", description = "Adds a message to an existing ticket thread.")
    @PostMapping("/{ticketId}/reply")
    public ResponseEntity<SupportTicket> replyToTicket(
            @Parameter(description = "ID of the ticket") @PathVariable Long ticketId,
            @RequestBody TicketMessage message) {
        String email = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(supportService.replyToTicketByUser(ticketId, message, email));
    }

    @Operation(summary = "Update Status", description = "Changes the ticket status. Admins can set any state; Users can only mark as CLOSED.")
    @PatchMapping("/{ticketId}/status")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('USER') and #status == 'CLOSED')")
    public ResponseEntity<SupportTicket> updateTicketStatus(
            @Parameter(description = "ID of the ticket") @PathVariable Long ticketId,
            @Parameter(description = "New status (OPEN, IN_PROGRESS, RESOLVED, CLOSED, ESCALATED)") @RequestParam String status) {
        return ResponseEntity.ok(supportService.updateTicketStatus(ticketId, status));
    }

    @Operation(summary = "List All Tickets (Admin)", description = "Retrieves all tickets across all users. Restricted to ADMIN.")
    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<SupportTicket>> getAllTickets(Pageable pageable) {
        return ResponseEntity.ok(supportService.getAllTickets(pageable));
    }

    @Operation(summary = "Admin Reply", description = "Adds a support staff reply to a ticket. Restricted to ADMIN.")
    @PostMapping("/{ticketId}/admin-reply")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SupportTicket> adminReply(
            @Parameter(description = "ID of the ticket") @PathVariable Long ticketId,
            @Parameter(description = "Reply message text") @RequestParam String message,
            @Parameter(description = "Staff ID / Name") @RequestParam String senderId) {
        return ResponseEntity.ok(supportService.adminReplyToTicket(ticketId, message, senderId));
    }
}

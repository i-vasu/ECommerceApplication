package com.app.customer_service.controllers;

import com.app.order.entities.SupportTicket;
import com.app.order.payloads.TicketDTO;
import com.app.order.entities.TicketMessage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Support Ticket", description = "Customer Support Ticket Management")
@SecurityRequirement(name = "E-Commerce Application")
public interface SupportApi {

    @Operation(summary = "Create Ticket", description = "Creates a new support ticket")
    @ApiResponse(responseCode = "200", description = "Ticket created")
    @PostMapping("/tickets")
    ResponseEntity<SupportTicket> createTicket(@RequestBody TicketDTO ticketDTO);

    @Operation(summary = "Reply to Ticket", description = "Adds a reply to an existing ticket")
    @ApiResponse(responseCode = "200", description = "Reply added")
    @PostMapping("/tickets/{ticketId}/reply")
    ResponseEntity<SupportTicket> replyToTicket(@PathVariable Long ticketId, @RequestBody TicketMessage message);

    @Operation(summary = "Get User Tickets", description = "Retrieves all tickets for a user with pagination")
    @ApiResponse(responseCode = "200", description = "Tickets retrieved")
    @GetMapping("/tickets/{email}")
    ResponseEntity<Page<SupportTicket>> getUserTickets(@PathVariable String email, Pageable pageable);

    @Operation(summary = "Get All Tickets (Paginated)", description = "Retrieves all tickets with pagination (Admin only). Default: page=0, size=20")
    @ApiResponse(responseCode = "200", description = "Tickets page retrieved")
    @GetMapping("/admin/tickets")
    ResponseEntity<Page<SupportTicket>> getAllTickets(Pageable pageable);

    @Operation(summary = "Update Ticket Status", description = "Updates the status of a ticket")
    @ApiResponse(responseCode = "200", description = "Status updated")
    @PutMapping("/admin/tickets/{ticketId}/status")
    ResponseEntity<SupportTicket> updateTicketStatus(@PathVariable Long ticketId, @RequestParam String status);
}

package com.app.support.domain;

import com.app.core.utils.ContentSanitizer;
import com.app.governance.audit.OperationalAudit;
import com.app.governance.audit.OperationalAuditRepo;
import com.app.support.entities.SupportTicket;
import com.app.support.payloads.TicketDTO;
import com.app.support.repositories.SupportTicketRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SupportServiceTest {

    @Mock
    private SupportTicketRepo ticketRepo;

    @Mock
    private OperationalAuditRepo auditRepo;

    @Mock
    private ContentSanitizer sanitizer;

    @InjectMocks
    private SupportService supportService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("Should create ticket with sanitized content")
    void testCreateTicket_Success() {
        // Setup
        TicketDTO ticketDTO = new TicketDTO();
        ticketDTO.setUserEmail("user@test.com");
        ticketDTO.setSubject("Test Subject <script>alert('xss')</script>");
        ticketDTO.setMessage("Test message <b>bold</b>");
        ticketDTO.setRelatedOrderId(123L);

        when(sanitizer.stripHtml(anyString())).thenReturn("Test Subject");
        when(sanitizer.sanitize(anyString())).thenReturn("Test message bold");
        when(auditRepo.findByEntityId(anyString())).thenReturn(new ArrayList<>());
        
        SupportTicket savedTicket = new SupportTicket();
        savedTicket.setTicketId(1L);
        savedTicket.setStatus("OPEN");
        when(ticketRepo.save(any(SupportTicket.class))).thenReturn(savedTicket);

        // Execute
        SupportTicket result = supportService.createTicket(ticketDTO);

        // Verify
        assertNotNull(result);
        assertEquals("OPEN", result.getStatus());
        verify(sanitizer).stripHtml(anyString());
        verify(sanitizer).sanitize(anyString());
        verify(ticketRepo).save(any(SupportTicket.class));
    }

    @Test
    @DisplayName("Should auto-categorize ticket based on audit logs")
    void testCreateTicket_AutoCategorization() {
        // Setup
        TicketDTO ticketDTO = new TicketDTO();
        ticketDTO.setUserEmail("user@test.com");
        ticketDTO.setSubject("Delivery Issue");
        ticketDTO.setMessage("Where is my order?");
        ticketDTO.setRelatedOrderId(456L);

        when(sanitizer.stripHtml(anyString())).thenReturn("Delivery Issue");
        when(sanitizer.sanitize(anyString())).thenReturn("Where is my order?");

        // Mock audit with SLA_VIOLATION
        OperationalAudit audit = new OperationalAudit();
        audit.setType("SLA_VIOLATION");
        audit.setDetail("Shipment delayed");
        when(auditRepo.findByEntityId("456")).thenReturn(List.of(audit));

        SupportTicket savedTicket = new SupportTicket();
        savedTicket.setCategory("LOGISTICS");
        when(ticketRepo.save(any(SupportTicket.class))).thenAnswer(invocation -> {
            SupportTicket ticket = invocation.getArgument(0);
            assertEquals("LOGISTICS", ticket.getCategory());
            return savedTicket;
        });

        // Execute
        SupportTicket result = supportService.createTicket(ticketDTO);

        // Verify
        verify(auditRepo).findByEntityId("456");
        verify(ticketRepo).save(argThat(ticket -> "LOGISTICS".equals(ticket.getCategory())));
    }

    @Test
    @DisplayName("Should update ticket status")
    void testUpdateTicketStatus() {
        // Setup
        Long ticketId = 1L;
        SupportTicket ticket = new SupportTicket();
        ticket.setTicketId(ticketId);
        ticket.setStatus("OPEN");

        when(ticketRepo.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(ticketRepo.save(any(SupportTicket.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Execute
        SupportTicket result = supportService.updateTicketStatus(ticketId, "CLOSED");

        // Verify
        assertEquals("CLOSED", result.getStatus());
        verify(ticketRepo).save(ticket);
    }

    @Test
    @DisplayName("Should check if order has open ticket")
    void testHasOpenTicketForOrder() {
        // Setup
        Long orderId = 789L;
        when(ticketRepo.existsByRelatedOrderIdAndStatusNot(orderId, "CLOSED")).thenReturn(true);

        // Execute
        boolean result = supportService.hasOpenTicketForOrder(orderId);

        // Verify
        assertTrue(result);
        verify(ticketRepo).existsByRelatedOrderIdAndStatusNot(orderId, "CLOSED");
    }

    @Test
    @DisplayName("Should handle admin reply and update status to IN_PROGRESS")
    void testAdminReplyToTicket() {
        // Setup
        Long ticketId = 1L;
        SupportTicket ticket = new SupportTicket();
        ticket.setTicketId(ticketId);
        ticket.setStatus("OPEN");
        ticket.setMessages(new ArrayList<>());

        when(ticketRepo.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(sanitizer.sanitize(anyString())).thenReturn("Admin response");
        when(ticketRepo.save(any(SupportTicket.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Execute
        SupportTicket result = supportService.adminReplyToTicket(ticketId, "Admin response", "admin@test.com");

        // Verify
        assertEquals("IN_PROGRESS", result.getStatus());
        assertEquals(1, result.getMessages().size());
        verify(ticketRepo).save(ticket);
    }
}

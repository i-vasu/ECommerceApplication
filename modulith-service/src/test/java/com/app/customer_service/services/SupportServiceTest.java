package com.app.customer_service.services;

import com.app.support.domain.SupportService;
import com.app.support.entities.SupportTicket;
import com.app.support.entities.TicketMessage;
import com.app.support.repositories.SupportTicketRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SupportServiceTest {

    @Mock
    private SupportTicketRepo ticketRepo;
    @Mock
    private com.app.governance.audit.OperationalAuditRepo auditRepo;
    @Mock
    private com.app.core.utils.ContentSanitizer sanitizer;

    @InjectMocks
    private SupportService supportService;

    private SupportTicket testTicket;
    private String userEmail = "customer@example.com";

    @BeforeEach
    void setUp() {
        testTicket = new SupportTicket();
        testTicket.setTicketId(1L);
        testTicket.setUserEmail(userEmail);
        testTicket.setSubject("Issue with Refund");
        testTicket.setStatus("OPEN");
        testTicket.setMessages(new ArrayList<>());
    }

    @Test
    void testCreateTicket() {
        when(ticketRepo.save(any(SupportTicket.class))).thenReturn(testTicket);
        when(sanitizer.stripHtml(anyString())).thenAnswer(i -> i.getArgument(0));
        when(sanitizer.sanitize(anyString())).thenAnswer(i -> i.getArgument(0));

        com.app.support.payloads.TicketDTO ticketDTO = new com.app.support.payloads.TicketDTO();
        ticketDTO.setUserEmail(userEmail);
        ticketDTO.setSubject("Issue");
        ticketDTO.setMessage("I have a problem");
        
        SupportTicket created = supportService.createTicket(ticketDTO);
        
        assertNotNull(created);
        assertEquals(userEmail, created.getUserEmail());
        verify(ticketRepo, times(1)).save(any());
    }

    @Test
    void testReplyToTicket_User() {
        when(ticketRepo.findById(1L)).thenReturn(Optional.of(testTicket));
        when(sanitizer.sanitize(anyString())).thenAnswer(i -> i.getArgument(0));
        
        TicketMessage msg = new TicketMessage();
        msg.setMessage("This is a reply");
        msg.setSenderType("USER");
        msg.setSenderId(userEmail);
        
        supportService.replyToTicket(1L, msg);
        
        verify(ticketRepo, times(1)).save(any());
        assertEquals("OPEN", testTicket.getStatus());
    }

    @Test
    void testReplyToTicket_Admin() {
        when(ticketRepo.findById(1L)).thenReturn(Optional.of(testTicket));
        when(sanitizer.sanitize(anyString())).thenAnswer(i -> i.getArgument(0));
        
        supportService.adminReplyToTicket(1L, "Admin reply", "admin@vaabhi.com");
        
        assertEquals("IN_PROGRESS", testTicket.getStatus());
    }

    @Test
    void testGetUserTickets() {
        org.springframework.data.domain.Page<SupportTicket> page = new org.springframework.data.domain.PageImpl<>(List.of(testTicket));
        when(ticketRepo.findByUserEmail(eq(userEmail), any())).thenReturn(page);
        
        org.springframework.data.domain.Page<SupportTicket> result = supportService.getUserTickets(userEmail, org.springframework.data.domain.PageRequest.of(0, 10));
        
        assertEquals(1, result.getContent().size());
        verify(ticketRepo).findByUserEmail(eq(userEmail), any());
    }

    @Test
    void testGetTicket_Found() {
        when(ticketRepo.findById(1L)).thenReturn(Optional.of(testTicket));
        
        SupportTicket found = supportService.getTicketById(1L);
        
        assertNotNull(found);
        assertEquals(1L, found.getTicketId());
    }

    @Test
    void testGetTicket_NotFound() {
        when(ticketRepo.findById(99L)).thenReturn(Optional.empty());
        
        assertThrows(RuntimeException.class, () -> supportService.getTicketById(99L));
    }
}

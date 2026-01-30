package com.app.customer_service.services;

import com.app.order.entities.SupportTicket;
import com.app.order.entities.TicketMessage;
import com.app.order.repositories.SupportTicketRepo;
import com.app.order.repositories.TicketMessageRepo;
import com.app.core.ResourceNotFoundException;
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
    private TicketMessageRepo messageRepo;

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
        testTicket.setStatus(SupportTicket.TicketStatus.OPEN);
        testTicket.setMessages(new ArrayList<>());
    }

    @Test
    void testCreateTicket() {
        when(ticketRepo.save(any(SupportTicket.class))).thenReturn(testTicket);
        
        SupportTicket created = supportService.createTicket(userEmail, "Issue", "I have a problem", null, "HIGH");
        
        assertNotNull(created);
        assertEquals(userEmail, created.getUserEmail());
        verify(ticketRepo, times(1)).save(any());
        verify(messageRepo, times(1)).save(any());
    }

    @Test
    void testReplyToTicket_User() {
        when(ticketRepo.findById(1L)).thenReturn(Optional.of(testTicket));
        
        supportService.replyToTicket(1L, "This is a reply", "USER", userEmail);
        
        verify(messageRepo, times(1)).save(any());
        assertEquals(SupportTicket.TicketStatus.OPEN, testTicket.getStatus());
    }

    @Test
    void testReplyToTicket_Admin() {
        when(ticketRepo.findById(1L)).thenReturn(Optional.of(testTicket));
        
        supportService.replyToTicket(1L, "Admin reply", "ADMIN", "admin@vaabhi.com");
        
        assertEquals(SupportTicket.TicketStatus.IN_PROGRESS, testTicket.getStatus());
    }

    @Test
    void testGetUserTickets() {
        List<SupportTicket> list = List.of(testTicket);
        when(ticketRepo.findByUserEmailOrderByCreatedAtDesc(userEmail)).thenReturn(list);
        
        List<SupportTicket> result = supportService.getUserTickets(userEmail);
        
        assertEquals(1, result.size());
        verify(ticketRepo).findByUserEmailOrderByCreatedAtDesc(userEmail);
    }

    @Test
    void testGetTicket_Found() {
        when(ticketRepo.findById(1L)).thenReturn(Optional.of(testTicket));
        
        SupportTicket found = supportService.getTicket(1L);
        
        assertNotNull(found);
        assertEquals(1L, found.getTicketId());
    }

    @Test
    void testGetTicket_NotFound() {
        when(ticketRepo.findById(99L)).thenReturn(Optional.empty());
        
        assertThrows(ResourceNotFoundException.class, () -> supportService.getTicket(99L));
    }
}

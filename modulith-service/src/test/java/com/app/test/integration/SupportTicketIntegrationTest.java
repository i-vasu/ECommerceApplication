package com.app.test.integration;

import com.app.support.entities.SupportTicket;
import com.app.support.repositories.SupportTicketRepo;
import com.app.support.domain.SupportService;
import com.app.test.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
public class SupportTicketIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private SupportService supportService;

    @Autowired
    private SupportTicketRepo ticketRepo;

    @Test
    void testSupportTicketLifecycle() {
        String email = "support_user@example.com";
        
        // 1. Create Ticket
        SupportTicket created = supportService.createTicket(
            email, 
            "Payment Failed", 
            "My payment went through but order not confirmed", 
            123L, 
            "HIGH"
        );
        
        assertThat(created.getTicketId()).isNotNull();
        assertThat(created.getSubject()).isEqualTo("Payment Failed");
        assertThat(created.getStatus()).isEqualTo(SupportTicket.TicketStatus.OPEN);
        
        // 2. User Reply
        supportService.replyToTicket(created.getTicketId(), "Any update?", "USER", email);
        
        // 3. Admin Reply
        supportService.replyToTicket(created.getTicketId(), "We are checking with our bank.", "ADMIN", "admin@vaabhi.com");
        
        // 4. Verify Status and Messages
        SupportTicket updated = supportService.getTicket(created.getTicketId());
        assertThat(updated.getStatus()).isEqualTo(SupportTicket.TicketStatus.IN_PROGRESS);
        assertThat(updated.getMessages()).hasSize(3); // Initial + 2 replies
        
        // 5. Close Ticket
        supportService.updateTicketStatus(created.getTicketId(), SupportTicket.TicketStatus.RESOLVED);
        assertThat(supportService.getTicket(created.getTicketId()).getStatus()).isEqualTo(SupportTicket.TicketStatus.RESOLVED);
        
        // 6. Get User Tickets
        List<SupportTicket> userTickets = supportService.getUserTickets(email);
        assertThat(userTickets).hasSize(1);
    }
}

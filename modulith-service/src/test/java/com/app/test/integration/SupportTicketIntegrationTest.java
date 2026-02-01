package com.app.test.integration;

import com.app.support.domain.SupportService;
import com.app.support.entities.SupportTicket;
import com.app.support.repositories.SupportTicketRepo;
import com.app.test.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

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
        com.app.support.payloads.TicketDTO ticketDTO = new com.app.support.payloads.TicketDTO();
        ticketDTO.setUserEmail(email);
        ticketDTO.setSubject("Payment Failed");
        ticketDTO.setMessage("My payment went through but order not confirmed");
        ticketDTO.setRelatedOrderId(123L);

        SupportTicket created = supportService.createTicket(ticketDTO);
        
        assertThat(created.getTicketId()).isNotNull();
        assertThat(created.getSubject()).isEqualTo("Payment Failed");
        assertThat(created.getStatus()).isEqualTo("OPEN");
        
        // 2. User Reply
        com.app.support.entities.TicketMessage userMsg = new com.app.support.entities.TicketMessage();
        userMsg.setSenderType("USER");
        userMsg.setSenderId(email);
        userMsg.setMessage("Any update?");
        supportService.replyToTicket(created.getTicketId(), userMsg);
        
        // 3. Admin Reply
        supportService.adminReplyToTicket(created.getTicketId(), "We are checking with our bank.", "admin@vaabhi.com");
        
        // 4. Verify Status and Messages
        SupportTicket updated = supportService.getTicketById(created.getTicketId());
        assertThat(updated.getStatus()).isEqualTo("IN_PROGRESS");
        assertThat(updated.getMessages()).hasSize(3); // Initial + 2 replies
        
        // 5. Close Ticket
        supportService.updateTicketStatus(created.getTicketId(), "RESOLVED");
        assertThat(supportService.getTicketById(created.getTicketId()).getStatus()).isEqualTo("RESOLVED");
        
        // 6. Get User Tickets
        org.springframework.data.domain.Page<SupportTicket> userTickets = supportService.getUserTickets(email, org.springframework.data.domain.PageRequest.of(0, 10));
        assertThat(userTickets.getContent()).hasSize(1);
    }
}

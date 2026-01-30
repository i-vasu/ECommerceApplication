package com.app.support.domain;

import com.app.support.repositories.SupportTicketRepo;
import com.app.support.entities.SupportTicket;
import com.app.support.entities.TicketMessage;
import com.app.support.payloads.TicketDTO;
import com.app.core.utils.ContentSanitizer;
import com.app.governance.audit.OperationalAuditRepo;
import com.app.governance.audit.OperationalAudit;
import com.app.config.AppConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SupportService {

    private final SupportTicketRepo ticketRepo;
    private final OperationalAuditRepo auditRepo;
    private final ContentSanitizer sanitizer;

    @Transactional
    public SupportTicket createTicket(TicketDTO ticketDTO) {
        SupportTicket ticket = new SupportTicket();
        ticket.setUserEmail(ticketDTO.getUserEmail());
        ticket.setSubject(sanitizer.stripHtml(ticketDTO.getSubject()));
        ticket.setRelatedOrderId(ticketDTO.getRelatedOrderId());
        ticket.setStatus("OPEN");
        ticket.setCreatedAt(LocalDateTime.now());

        // Autonomous Categorization
        if (ticket.getRelatedOrderId() != null) {
            autoCategorize(ticket);
        }

        TicketMessage message = new TicketMessage();
        message.setTicket(ticket);
        message.setSenderType("USER");
        message.setSenderId(ticketDTO.getUserEmail());
        message.setMessage(sanitizer.sanitize(ticketDTO.getMessage()));
        message.setTimestamp(LocalDateTime.now());

        ticket.getMessages().add(message);

        return ticketRepo.save(ticket);
    }

    @Transactional
    public SupportTicket replyToTicket(Long ticketId, TicketMessage message) {
        SupportTicket ticket = ticketRepo.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));

        message.setTicket(ticket);
        message.setMessage(sanitizer.sanitize(message.getMessage()));
        message.setTimestamp(LocalDateTime.now());

        ticket.getMessages().add(message);

        // Optional logic regarding status
        if (!"CLOSED".equals(ticket.getStatus())
                && "USER".equalsIgnoreCase(message.getSenderType())
                && !"IN_PROGRESS".equals(ticket.getStatus())) {
            // e.g. set to IN_PROGRESS if user replies? Or maybe stays OPEN.
        }

        return ticketRepo.save(ticket);
    }

    @Transactional
    public SupportTicket adminReplyToTicket(Long ticketId, String message, String senderId) {
        SupportTicket ticket = ticketRepo.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));

        TicketMessage msg = new TicketMessage();
        msg.setTicket(ticket);
        msg.setSenderType("ADMIN");
        msg.setSenderId(senderId);
        msg.setMessage(sanitizer.sanitize(message));
        msg.setTimestamp(LocalDateTime.now());

        ticket.getMessages().add(msg);

        if (!"CLOSED".equals(ticket.getStatus())) {
            ticket.setStatus("IN_PROGRESS");
        }

        return ticketRepo.save(ticket);
    }

    @Transactional(readOnly = true)
    public Page<SupportTicket> getUserTickets(String email, Pageable pageable) {
        return ticketRepo.findByUserEmail(email, capPageSize(pageable));
    }

    @Transactional(readOnly = true)
    public Page<SupportTicket> getAllTickets(Pageable pageable) {
        return ticketRepo.findAll(capPageSize(pageable));
    }

    private Pageable capPageSize(Pageable pageable) {
        if (pageable.getPageSize() > AppConstants.MAX_PAGE_SIZE) {
            return org.springframework.data.domain.PageRequest.of(pageable.getPageNumber(),
                    AppConstants.MAX_PAGE_SIZE, pageable.getSort());
        }
        return pageable;
    }

    @Transactional(readOnly = true)
    public SupportTicket getTicketById(Long id) {
        return ticketRepo.findById(id).orElseThrow(() -> new RuntimeException("Ticket not found"));
    }

    @Transactional
    public SupportTicket updateTicketStatus(Long ticketId, String status) {
        SupportTicket ticket = ticketRepo.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));
        ticket.setStatus(status);
        return ticketRepo.save(ticket);
    }

    private void autoCategorize(SupportTicket ticket) {
        String entityId = String.valueOf(ticket.getRelatedOrderId());
        java.util.List<OperationalAudit> audits = auditRepo.findByEntityId(entityId);

        for (OperationalAudit audit : audits) {
            String type = audit.getType();
            if ("SLA_VIOLATION".equals(type)) {
                ticket.setCategory("LOGISTICS");
                return;
            }
            if (audit.getDetail().contains("Shipment")) {
                ticket.setCategory("LOGISTICS");
                return;
            }
            if (audit.getDetail().contains("Refund")) {
                ticket.setCategory("REFUND");
                return;
            }
        }
    }

    @Transactional(readOnly = true)
    public boolean hasOpenTicketForOrder(Long orderId) {
        return ticketRepo.existsByRelatedOrderIdAndStatusNot(orderId, "CLOSED");
    }
}

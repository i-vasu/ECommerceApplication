package com.app.customer_service.repositories;

import com.app.order.entites.SupportTicket;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SupportTicketRepo extends JpaRepository<SupportTicket, Long> {
    List<SupportTicket> findByUserEmail(String userEmail);
}

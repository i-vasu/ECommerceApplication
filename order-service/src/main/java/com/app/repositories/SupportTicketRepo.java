package com.app.repositories;

import com.app.entites.SupportTicket;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SupportTicketRepo extends JpaRepository<SupportTicket, Long> {
    List<SupportTicket> findByUserEmail(String userEmail);
}

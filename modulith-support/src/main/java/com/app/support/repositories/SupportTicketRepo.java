package com.app.support.repositories;

import com.app.support.entities.SupportTicket;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SupportTicketRepo extends JpaRepository<SupportTicket, Long> {
    Page<SupportTicket> findByUserEmail(String email, Pageable pageable);

    boolean existsByRelatedOrderIdAndStatusNot(Long orderId, String status);
}

package com.app.support.repositories;

import com.app.support.entities.TicketMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TicketMessageRepo extends JpaRepository<TicketMessage, Long> {
}

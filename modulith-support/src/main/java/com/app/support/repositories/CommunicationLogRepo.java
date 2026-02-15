package com.app.support.repositories;

import com.app.support.entities.CommunicationLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommunicationLogRepo extends JpaRepository<CommunicationLog, Long> {
    List<CommunicationLog> findByRecipientEmailOrderByCreatedAtDesc(String email);
}

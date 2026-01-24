package com.app.order.repositories;

import com.app.order.entities.ReturnRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReturnRequestRepo extends JpaRepository<ReturnRequest, Long> {
    List<ReturnRequest> findByOrderEmail(String email);
}

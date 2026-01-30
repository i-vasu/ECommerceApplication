package com.app.support.repositories;

import com.app.support.entities.ReturnRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReturnRequestRepo extends JpaRepository<ReturnRequest, Long> {
    List<ReturnRequest> findByUserEmail(String email);

    long countByUserEmail(String email);

    boolean existsByOrderIdAndStatusNot(Long orderId, String status);
}

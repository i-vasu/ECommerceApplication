package com.app.finance.repositories;

import com.app.finance.entities.Refund;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefundRepo extends JpaRepository<Refund, Long> {
    Optional<Refund> findByPgRefundId(String pgRefundId);
}

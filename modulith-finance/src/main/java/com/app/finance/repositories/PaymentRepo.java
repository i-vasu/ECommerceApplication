package com.app.finance.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.app.finance.entities.Payment;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepo extends JpaRepository<Payment, Long> {

    Payment findByPgOrderId(String pgOrderId);
    
    Optional<Payment> findByPgPaymentId(String pgPaymentId);

    List<Payment> findByOrderId(Long orderId);
}

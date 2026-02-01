package com.app.finance.repositories;

import com.app.finance.entities.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepo extends JpaRepository<Payment, Long> {

    Payment findByPgOrderId(String pgOrderId);
    
    Optional<Payment> findByPgPaymentId(String pgPaymentId);

    List<Payment> findByOrderId(Long orderId);
}

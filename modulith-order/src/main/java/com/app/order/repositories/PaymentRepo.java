package com.app.order.repositories;

import com.app.order.entities.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepo extends JpaRepository<Payment, Long> {
    Payment findByPgOrderId(String pgOrderId);

    java.util.Optional<Payment> findByPgPaymentId(String pgPaymentId);
}

package com.app.order.repositories;

import com.app.order.entites.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepo extends JpaRepository<Payment, Long> {
    com.app.order.entites.Payment findByPgOrderId(String pgOrderId);
}

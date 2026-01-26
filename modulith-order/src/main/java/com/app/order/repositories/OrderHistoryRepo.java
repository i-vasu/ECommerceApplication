package com.app.order.repositories;

import com.app.order.entities.OrderHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface OrderHistoryRepo extends JpaRepository<OrderHistory, Long> {
    List<OrderHistory> findByOrderOrderIdOrderByUpdatedAtDesc(Long orderId);
}

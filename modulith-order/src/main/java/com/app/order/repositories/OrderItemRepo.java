package com.app.order.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.app.order.entities.OrderItem;

@Repository
public interface OrderItemRepo extends JpaRepository<OrderItem, Long> {

}

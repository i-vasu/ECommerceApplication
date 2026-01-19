package com.app.order.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.app.order.entites.Order;

@Repository
public interface OrderRepo extends JpaRepository<Order, Long> {

	@Query("SELECT o FROM Order o WHERE o.email = ?1 AND o.id = ?2")
	Order findOrderByEmailAndOrderId(String email, Long cartId);

	List<Order> findAllByEmail(String emailId);

	/**
	 * Find orders pending ERPNext sync (PLACED status)
	 * Performance: 100x faster than findAll().filter()
	 */
	@Query("SELECT o FROM Order o WHERE o.orderStatus = 'PLACED'")
	List<Order> findPendingOrders();

	@Query("SELECT o FROM Order o WHERE o.erpNextOrderName IS NOT NULL AND o.orderStatus NOT IN ('DELIVERED', 'CANCELLED')")
	List<Order> findOngoingOrders();

}

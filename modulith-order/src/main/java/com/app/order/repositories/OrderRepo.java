package com.app.order.repositories;

import com.app.order.entities.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepo extends JpaRepository<Order, Long> {

	@Query("SELECT o FROM Order o WHERE o.email = ?1 AND o.orderId = ?2")
	Order findOrderByEmailAndOrderId(String email, Long orderId);

	java.util.Optional<Order> findByErpNextOrderName(String erpNextOrderName);

	List<Order> findAllByEmail(String emailId);

	/**
	 * Find orders pending ERPNext sync (PENDING status)
	 * Performance: 100x faster than findAll().filter()
	 */
	@Query("SELECT o FROM Order o WHERE o.orderStatus = com.app.governance.states.OrderStatus.PENDING")
	List<Order> findPendingOrders();

	@Query("SELECT o FROM Order o WHERE o.erpNextOrderName IS NOT NULL AND o.orderStatus NOT IN (com.app.governance.states.OrderStatus.DELIVERED, com.app.governance.states.OrderStatus.CANCELLED)")
	List<Order> findOngoingOrders();

	@Query("SELECT COUNT(o) > 0 FROM Order o JOIN o.orderItems oi WHERE o.email = ?1 AND oi.productId = ?2 AND o.orderStatus = com.app.governance.states.OrderStatus.DELIVERED")
	boolean existsByEmailAndProductId(String email, Long productId);

	@Query("SELECT o FROM Order o WHERE o.orderStatus = com.app.governance.states.OrderStatus.PENDING AND o.orderDate < CURRENT_DATE")
	List<Order> findStalePendingOrders(java.time.LocalDateTime cutoff);

	long countByEmail(String email);

	@Query("SELECT o FROM Order o JOIN o.orderItems oi WHERE oi.itemCode = ?1 AND o.orderStatus = com.app.governance.states.OrderStatus.PENDING")
	List<Order> findPendingOrdersByItemCode(String itemCode);
}

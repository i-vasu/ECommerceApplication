package com.app.order.repositories;

import com.app.order.entities.OrderVendor;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VendorRepo extends JpaRepository<OrderVendor, Long> {
}

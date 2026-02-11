package com.app.logistics.repositories;

import com.app.logistics.entities.PurchaseOrder;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PurchaseOrderRepo extends JpaRepository<PurchaseOrder, Long> {
}

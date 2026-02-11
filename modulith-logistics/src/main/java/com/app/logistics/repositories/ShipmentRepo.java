package com.app.logistics.repositories;

import com.app.logistics.entities.Shipment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ShipmentRepo extends JpaRepository<Shipment, Long> {
    Shipment findByOrderId(Long orderId);

    @Query("SELECT s FROM Shipment s WHERE s.status NOT IN ('DELIVERED', 'CANCELLED', 'RETURNED')")
    List<Shipment> findActiveShipments();
}

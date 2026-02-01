package com.app.logistics.repositories;

import com.app.logistics.entities.Shipment;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ShipmentRepo extends org.springframework.data.repository.CrudRepository<Shipment, Long> {
    Shipment findByOrderId(Long orderId);

    @Query("SELECT * FROM shipments WHERE status NOT IN ('DELIVERED', 'CANCELLED', 'RETURNED')")
    List<Shipment> findActiveShipments();
}

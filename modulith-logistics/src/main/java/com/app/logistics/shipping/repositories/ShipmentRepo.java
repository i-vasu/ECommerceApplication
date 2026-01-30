package com.app.logistics.shipping.repositories;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

import com.app.logistics.entities.Shipment;

import java.util.List;

@Repository
public interface ShipmentRepo extends ListCrudRepository<Shipment, Long> {

    Shipment findByOrderId(Long orderId);

    @Query("SELECT * FROM shipments WHERE status NOT IN ('DELIVERED', 'CANCELLED', 'RETURNED')")
    List<Shipment> findActiveShipments();
}

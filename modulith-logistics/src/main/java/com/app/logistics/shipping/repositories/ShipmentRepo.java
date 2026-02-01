package com.app.logistics.shipping.repositories;

import com.app.logistics.entities.Shipment;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository("shippingShipmentRepo")
public interface ShipmentRepo extends ListCrudRepository<Shipment, Long> {

    Shipment findByOrderId(Long orderId);

    @Query("SELECT * FROM shipments WHERE status NOT IN ('DELIVERED', 'CANCELLED', 'RETURNED')")
    List<Shipment> findActiveShipments();
}

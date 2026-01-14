package com.app.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.app.entites.Shipment;

public interface ShipmentRepo extends JpaRepository<Shipment, Long> {

}

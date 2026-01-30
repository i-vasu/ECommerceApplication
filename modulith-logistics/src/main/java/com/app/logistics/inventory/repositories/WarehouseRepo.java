package com.app.logistics.inventory.repositories;

import com.app.logistics.inventory.entities.Warehouse;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WarehouseRepo extends ListCrudRepository<Warehouse, Long> {
    List<Warehouse> findByActiveTrue();
}

package com.app.catalog.repositories;

import com.app.catalog.entities.GtsPropertyValue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GtsPropertyValueRepository extends JpaRepository<GtsPropertyValue, Long> {
    Optional<GtsPropertyValue> findByCodeAndProperty_PropertyId(String valueCode, Long propertyId);
}

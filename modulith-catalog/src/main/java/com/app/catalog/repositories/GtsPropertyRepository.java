package com.app.catalog.repositories;

import com.app.catalog.entities.GtsProperty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GtsPropertyRepository extends JpaRepository<GtsProperty, Long> {
    Optional<GtsProperty> findByCode(String code);
}

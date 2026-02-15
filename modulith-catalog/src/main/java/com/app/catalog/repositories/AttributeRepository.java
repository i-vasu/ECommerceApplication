package com.app.catalog.repositories;

import com.app.catalog.entities.Attribute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AttributeRepository extends JpaRepository<Attribute, Long> {
    Optional<Attribute> findByCode(String code);
    Optional<Attribute> findByName(String name);
}

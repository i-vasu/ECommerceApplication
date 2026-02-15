package com.app.catalog.repositories;

import com.app.catalog.entities.AttributeValue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AttributeValueRepository extends JpaRepository<AttributeValue, Long> {
    Optional<AttributeValue> findByCodeAndAttribute_Code(String valueCode, String attributeCode);
}

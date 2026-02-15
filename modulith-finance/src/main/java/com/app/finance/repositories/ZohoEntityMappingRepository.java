package com.app.finance.repositories;

import com.app.finance.entities.ZohoEntityMapping;
import com.app.finance.entities.ZohoEntityMapping.ZohoEntityType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ZohoEntityMappingRepository extends JpaRepository<ZohoEntityMapping, Long> {

    Optional<ZohoEntityMapping> findByEntityTypeAndLocalId(ZohoEntityType entityType, String localId);

    boolean existsByEntityTypeAndLocalId(ZohoEntityType entityType, String localId);
}

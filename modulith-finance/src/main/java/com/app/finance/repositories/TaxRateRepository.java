package com.app.finance.repositories;

import com.app.finance.entities.TaxRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TaxRateRepository extends JpaRepository<TaxRate, Long> {
    Optional<TaxRate> findByZohoTaxId(String zohoTaxId);
}

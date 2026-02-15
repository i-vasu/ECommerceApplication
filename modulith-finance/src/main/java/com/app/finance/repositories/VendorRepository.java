package com.app.finance.repositories;

import com.app.finance.entities.FinanceVendor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VendorRepository extends JpaRepository<FinanceVendor, Long> {
    Optional<FinanceVendor> findByEmail(String email);
    Optional<FinanceVendor> findByGstin(String gstin);
    List<FinanceVendor> findByCategory(FinanceVendor.VendorCategory category);
    List<FinanceVendor> findByActiveTrue();
}

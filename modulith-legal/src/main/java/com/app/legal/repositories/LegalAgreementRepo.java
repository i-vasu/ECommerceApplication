package com.app.legal.repositories;

import com.app.legal.entities.LegalAgreement;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface LegalAgreementRepo extends JpaRepository<LegalAgreement, Long> {
    Optional<LegalAgreement> findByTypeAndActiveTrue(String type);
}

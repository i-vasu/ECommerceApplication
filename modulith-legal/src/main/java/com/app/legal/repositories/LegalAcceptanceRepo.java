package com.app.legal.repositories;

import com.app.legal.entities.LegalAcceptance;
import com.app.legal.entities.LegalAgreement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LegalAcceptanceRepo extends JpaRepository<LegalAcceptance, Long> {
    List<LegalAcceptance> findByEmail(String email);
    Optional<LegalAcceptance> findByEmailAndAgreementAndVersionAccepted(String email, LegalAgreement agreement, String version);
}

package com.app.legal.services;

import com.app.legal.entities.LegalAcceptance;
import com.app.legal.entities.LegalAgreement;
import com.app.legal.repositories.LegalAcceptanceRepo;
import com.app.legal.repositories.LegalAgreementRepo;
import com.app.core.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LegalService {

    private final LegalAgreementRepo agreementRepo;
    private final LegalAcceptanceRepo acceptanceRepo;

    public List<LegalAgreement> getAllAgreements() {
        return agreementRepo.findAll();
    }

    public Optional<LegalAgreement> getActiveAgreement(String type) {
        return agreementRepo.findByTypeAndActiveTrue(type);
    }

    @Transactional
    public LegalAgreement saveAgreement(LegalAgreement agreement) {
        if (agreement.isActive()) {
            // Deactivate other agreements of the same type
            agreementRepo.findByTypeAndActiveTrue(agreement.getType())
                .ifPresent(existing -> {
                    if (agreement.getId() == null || !existing.getId().equals(agreement.getId())) {
                        existing.setActive(false);
                        agreementRepo.save(existing);
                    }
                });
        }
        return agreementRepo.save(agreement);
    }

    @Transactional
    public void acceptAgreement(String email, String type, String ipAddress, String userAgent) {
        LegalAgreement agreement = agreementRepo.findByTypeAndActiveTrue(type)
            .orElseThrow(() -> new ResourceNotFoundException("LegalAgreement", "type", type));

        // Check if already accepted this version
        if (acceptanceRepo.findByEmailAndAgreementAndVersionAccepted(email, agreement, agreement.getVersion()).isPresent()) {
            return;
        }

        LegalAcceptance acceptance = new LegalAcceptance();
        acceptance.setEmail(email);
        acceptance.setAgreement(agreement);
        acceptance.setVersionAccepted(agreement.getVersion());
        acceptance.setIpAddress(ipAddress);
        acceptance.setUserAgent(userAgent);
        
        acceptanceRepo.save(acceptance);
    }
}

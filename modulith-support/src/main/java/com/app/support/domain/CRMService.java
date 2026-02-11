package com.app.support.domain;

import com.app.support.entities.CustomerProfile;
import com.app.support.repositories.CustomerProfileRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CRMService {

    private final CustomerProfileRepo profileRepo;

    @Transactional(readOnly = true)
    public CustomerProfile getProfile(String email) {
        return profileRepo.findById(email)
                .orElseGet(() -> {
                    CustomerProfile newProfile = new CustomerProfile();
                    newProfile.setEmail(email);
                    return newProfile;
                });
    }

    @Transactional
    public CustomerProfile updateMeasurements(String email, Map<String, Object> measurements) {
        CustomerProfile profile = getProfile(email);
        profile.setMeasurements(measurements);
        return profileRepo.save(profile);
    }

    @Transactional
    public void addRewardPoints(String email, Integer points) {
        CustomerProfile profile = getProfile(email);
        profile.setRewardPoints(profile.getRewardPoints() + points);
        
        // Tier upgrade logic
        if (profile.getRewardPoints() > 5000) {
            profile.setTier(CustomerProfile.CustomerTier.PLATINUM);
        } else if (profile.getRewardPoints() > 1000) {
            profile.setTier(CustomerProfile.CustomerTier.GOLD);
        }
        
        profileRepo.save(profile);
    }
}

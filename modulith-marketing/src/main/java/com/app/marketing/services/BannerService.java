package com.app.marketing.services;

import com.app.marketing.entities.HeroBanner;
import com.app.marketing.repositories.BannerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BannerService {

    private final BannerRepository bannerRepository;

    public List<HeroBanner> getAllBanners() {
        return bannerRepository.findAllByOrderByDisplayOrderAsc();
    }

    public List<HeroBanner> getActiveBanners() {
        return bannerRepository.findByIsActiveTrueOrderByDisplayOrderAsc();
    }

    public HeroBanner createBanner(HeroBanner banner) {
        return bannerRepository.save(banner);
    }
    
    public HeroBanner updateBanner(Long id, HeroBanner updated) {
        HeroBanner existing = bannerRepository.findById(id).orElseThrow(() -> new RuntimeException("Banner not found"));
        existing.setTitle(updated.getTitle());
        existing.setImageUrl(updated.getImageUrl());
        existing.setTargetUrl(updated.getTargetUrl());
        existing.setDisplayOrder(updated.getDisplayOrder());
        existing.setActive(updated.isActive());
        existing.setStartDate(updated.getStartDate());
        existing.setEndDate(updated.getEndDate());
        return bannerRepository.save(existing);
    }

    public void deleteBanner(Long id) {
        bannerRepository.deleteById(id);
    }
}

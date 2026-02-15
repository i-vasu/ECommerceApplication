package com.app.marketing.repositories;

import com.app.marketing.entities.HeroBanner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BannerRepository extends JpaRepository<HeroBanner, Long> {
    List<HeroBanner> findByIsActiveTrueOrderByDisplayOrderAsc();
    List<HeroBanner> findAllByOrderByDisplayOrderAsc();
}

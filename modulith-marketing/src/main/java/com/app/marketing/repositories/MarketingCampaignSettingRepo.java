package com.app.marketing.repositories;

import com.app.marketing.entities.MarketingCampaignSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MarketingCampaignSettingRepo extends JpaRepository<MarketingCampaignSetting, Long> {
    Optional<MarketingCampaignSetting> findByCampaignName(String campaignName);
}

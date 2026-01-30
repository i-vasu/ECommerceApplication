package com.app.marketing.repositories;

import com.app.marketing.entities.CampaignLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CampaignLinkRepo extends JpaRepository<CampaignLink, UUID> {
}

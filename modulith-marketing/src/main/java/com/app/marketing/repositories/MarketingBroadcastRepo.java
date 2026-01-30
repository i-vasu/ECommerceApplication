package com.app.marketing.repositories;

import com.app.marketing.entities.MarketingBroadcast;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MarketingBroadcastRepo extends JpaRepository<MarketingBroadcast, Integer> {
}

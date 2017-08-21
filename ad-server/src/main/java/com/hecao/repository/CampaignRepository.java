package com.hecao.repository;

import com.hecao.domain.Campaign;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CampaignRepository extends JpaRepository<Campaign, Long> {
    Campaign findByCampaignId(long campaignId);
}

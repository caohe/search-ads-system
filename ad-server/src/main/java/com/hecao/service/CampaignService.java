package com.hecao.service;

import com.hecao.domain.Campaign;

public interface CampaignService {
    Campaign getCampaign(long campaignId);
    void saveCampaign(Campaign campaign);
}

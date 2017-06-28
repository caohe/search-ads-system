package com.bihju.service;

import com.bihju.domain.Campaign;

public interface CampaignService {
    Campaign getCampaign(long campaignId);
    void updateCampaign(Campaign campaign);
}

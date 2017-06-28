package com.bihju.service.impl;

import com.bihju.domain.Campaign;
import com.bihju.repository.CampaignRepository;
import com.bihju.service.CampaignService;
import lombok.extern.log4j.Log4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Log4j
public class CampaignServiceImp implements CampaignService {
    private CampaignRepository campaignRepository;

    @Autowired
    public CampaignServiceImp(CampaignRepository campaignRepository) {
        this.campaignRepository = campaignRepository;
    }

    @Override
    public Campaign getCampaign(long campaignId) {
        return campaignRepository.findByCampaignId(campaignId);
    }

    @Override
    public void updateCampaign(Campaign campaign) {
        campaignRepository.save(campaign);
    }
}

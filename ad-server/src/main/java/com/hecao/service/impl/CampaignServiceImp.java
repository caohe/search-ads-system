package com.hecao.service.impl;

import com.hecao.domain.Campaign;
import com.hecao.repository.CampaignRepository;
import com.hecao.service.CampaignService;
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
    public void saveCampaign(Campaign campaign) {
        Campaign foundCampaign = campaignRepository.findByCampaignId(campaign.getCampaignId());
        if (foundCampaign != null) {
            foundCampaign.setBudget(campaign.getBudget());
            campaignRepository.save(foundCampaign);
        } else {
            campaignRepository.save(campaign);
        }
    }
}

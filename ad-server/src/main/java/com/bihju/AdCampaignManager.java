package com.bihju;

import com.bihju.domain.Ad;
import com.bihju.domain.Campaign;
import com.bihju.service.AdService;
import com.bihju.service.CampaignService;
import lombok.extern.log4j.Log4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@Log4j
public class AdCampaignManager {
    private static double MIN_PRICE_THRESHOLD = 0.01;

    private AdService adService;
    private CampaignService campaignService;

    @Autowired
    public AdCampaignManager(AdService adService, CampaignService campaignService) {
        this.adService = adService;
        this.campaignService = campaignService;
    }

    public List<Ad> deDupeAdsByCampaignId(List<Ad> level1Ads) {
        List<Ad> deDupedAds = new ArrayList<>();
        Set<Long> campaignIds = new HashSet<>();
        for (Ad ad : level1Ads) {
            if (!campaignIds.contains(ad.getCampaignId())) {
                deDupedAds.add(ad);
                campaignIds.add(ad.getCampaignId());
            }
        }

        return deDupedAds;
    }

    public List<Ad> applyBudget(List<Ad> inputAds) {
        List<Ad> finalAds = new ArrayList<>();
        for (Ad inputAd : inputAds) {
            Ad ad = adService.getAd(inputAd.getAdId());
            Long campaignId = ad.getCampaignId();
            Campaign campaign = campaignService.getCampaign(campaignId);
            if (campaign == null) {
                log.error("Failed to find campaign, campaignId = " + campaignId);
                continue;
            }
            log.info("ad.costPerClick = " + ad.getCostPerClick());
            log.info("campaignId = " + campaignId);
            log.info("budget left = " + campaign.getBudget());
            if (ad.getCostPerClick() <= campaign.getBudget() && ad.costPerClick >= MIN_PRICE_THRESHOLD) {
                finalAds.add(ad);
                campaign.setBudget(campaign.getBudget() - ad.costPerClick);
                campaignService.saveCampaign(campaign);
            }
        }

        return finalAds;
    }
}

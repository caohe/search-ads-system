package com.bihju;

import com.bihju.domain.Ad;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AdsAllocation {
    private final static double mainLinePriceThreshold = 4.5;
    private final static double mainLineRankScoreThreshold = 1.0;

    protected AdsAllocation() {
    }

    public void allocateAds(List<Ad> ads) {
        for (Ad ad : ads) {
            if (ad.costPerClick >= mainLinePriceThreshold && ad.rankScore >= mainLineRankScoreThreshold) {
                ad.position = 1;
            } else {
                ad.position = 2;
            }
        }
    }
}

package com.bihju.utility;

import com.bihju.domain.Ad;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AdPricing {
    public void setCostPerClick(List<Ad> ads) {
        for (int i = 0; i < ads.size(); i++) {
            if (i < ads.size() - 1) {
                ads.get(i).costPerClick = ads.get(i + 1).rankScore / ads.get(i).qualityScore + 0.01;
            } else {
                ads.get(i).costPerClick = ads.get(i).bidPrice;
            }
        }
    }
}

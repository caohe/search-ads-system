package com.hecao.utility;

import com.hecao.domain.Ad;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class AdFilter {
    private static final int MINIMUM_ADS = 4;

    public List<Ad> filterAds(List<Ad> ads, int k) {
        if (ads.size() <= MINIMUM_ADS) {
            return ads;
        }

        List<Ad> filteredAds = new ArrayList<>();
        for (int i = 0; i < Math.min(k, ads.size()); i++) {
            filteredAds.add(ads.get(i));
        }

        return filteredAds;
    }
}

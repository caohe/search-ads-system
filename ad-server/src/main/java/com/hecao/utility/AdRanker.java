package com.hecao.utility;

import com.hecao.domain.Ad;
import lombok.extern.log4j.Log4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@Component
@Log4j
public class AdRanker {
    private static double d = 0.25;

    public List<Ad> rankAds(List<Ad> level0Ads) {
        for (Ad ad : level0Ads) {
            ad.qualityScore = d * ad.pClick + (1.0 - d) * ad.relevanceScore;
            ad.rankScore = ad.qualityScore * ad.bidPrice;
        }

        Collections.sort(level0Ads, new Comparator<Ad>() {
            @Override
            public int compare(Ad ad1, Ad ad2) {
                if (ad1.rankScore < ad2.rankScore) {
                    return -1;
                } else if (ad1.rankScore > ad2.rankScore) {
                    return 1;
                } else {
                    return 0;
                }
            }
        });

        for (Ad ad : level0Ads) {
            log.info("Rankscore = " + ad.getRankScore());
        }

        return level0Ads;
    }
}

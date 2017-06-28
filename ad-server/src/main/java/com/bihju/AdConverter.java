package com.bihju;

import com.bihju.domain.Ad;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class AdConverter {
    public static Ad cloneAd(com.bihju.adindex.Ad ad) {
        Ad newAd = new Ad();
        newAd.setAdId(ad.getAdId());
        newAd.setCampaignId(ad.getCampaignId());
        List<String> keywords = new ArrayList<>();
        int size = ad.getKeyWordsList().size();
        for (int i = 0; i < size; i++) {
            keywords.add(ad.getKeyWords(i));
        }
        newAd.setKeyWords(String.join(",", keywords));
        newAd.setRelevanceScore(ad.getRelevanceScore());
        newAd.setRankScore(ad.getRankScore());
        newAd.setPClick(ad.getPClick());
        newAd.setBidPrice(ad.getBidPrice());
        newAd.setRankScore(ad.getRankScore());
        newAd.setQualityScore(ad.getQualityScore());
        newAd.setCostPerClick(ad.getCostPerClick());
        newAd.setPosition(ad.getPosition());
        newAd.setTitle(ad.getTitle());
        newAd.setPrice(ad.getPrice());
        newAd.setThumbnail(ad.getThumbnail());
        newAd.setDescription(ad.getDescription());
        newAd.setBrand(ad.getBrand());
        newAd.setDetailUrl(ad.getDetailUrl());
        newAd.setQuery(ad.getQuery());
        newAd.setCategory(ad.getCategory());
        return newAd;
    }
}

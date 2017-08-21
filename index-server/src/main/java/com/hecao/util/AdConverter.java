package com.hecao.util;

import com.hecao.domain.Ad;

public class AdConverter {
    public static com.hecao.adindex.Ad.Builder cloneAd(Ad ad) {
        com.hecao.adindex.Ad.Builder adBuilder = com.hecao.adindex.Ad.newBuilder();
        adBuilder.setAdId(ad.getAdId());
        adBuilder.setCampaignId(ad.getCampaignId());
        String keyWords = ad.getKeyWords();
        String[] keyWordList = keyWords.split(",");
        for (String keyWord : keyWordList) {
            adBuilder.addKeyWords(keyWord);
        }
        adBuilder.setBidPrice(ad.getBidPrice());
        adBuilder.setPrice(ad.getPrice());
        adBuilder.setThumbnail(ad.getThumbnail());
        adBuilder.setDescription(ad.getDescription());
        adBuilder.setBrand(ad.getBrand());
        adBuilder.setDetailUrl(ad.getDetailUrl());
        adBuilder.setCategory(ad.getCategory());
        adBuilder.setTitle(ad.getTitle());
        return adBuilder;
    }

}

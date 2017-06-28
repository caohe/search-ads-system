package com.bihju;

import com.bihju.adindex.Query;
import com.bihju.domain.Ad;
import com.bihju.domain.Campaign;
import com.bihju.service.AdService;
import com.bihju.service.CampaignService;
import lombok.extern.log4j.Log4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Log4j
@Component
public class AdEngine {
    private static final String AD_ID = "adId";
    private static final String CAMPAIGN_ID = "campaignId";
    private static final String BRAND = "brand";
    private static final String PRICE = "price";
    private static final String THUMBNAIL = "thumbnail";
    private static final String TITLE = "title";
    private static final String DETAIL_URL = "detailUrl";
    private static final String BID_PRICE = "bidPrice";
    private static final String P_CLICK = "pClick";
    private static final String CATEGORY = "category";
    private static final String DESCRIPTION = "description";
    private static final String KEY_WORDS = "keyWords";
    private static final String BUDGET = "budget";
    private static final int INDEX_SERVER_TIMEOUT = 1000;

    private IndexBuilder indexBuilder;
    private AdService adService;
    private CampaignService campaignService;
    private QueryParser queryParser;
    private AdConverter adConverter;
    private AdRanker adRanker;
    private AdFilter adFilter;
    private AdCampaignManager adCampaignManager;
    private AdPricing adPricing;

    @Value("${index.server1.address}")
    private String server1;
    @Value("${index.server2.address}")
    private String server2;
    @Value("${index.server1.port}")
    private int port1;
    @Value("${index.server2.port}")
    private int port2;
    @Value("${ad_file_path1}")
    private String adFilePath1;
    @Value("${ad_file_path2}")
    private String adFilePath2;
    @Value("${campaign_file_path}")
    private String campaignFilePath;

    @Autowired
    public AdEngine(IndexBuilder indexBuilder, AdService adService, CampaignService campaignService,
                    QueryParser queryParser, AdConverter adConverter, AdRanker adRanker, AdFilter adFilter,
                    AdCampaignManager adCampaignManager, AdPricing adPricing) {
        this.indexBuilder = indexBuilder;
        this.adService = adService;
        this.campaignService = campaignService;
        this.queryParser = queryParser;
        this.adConverter = adConverter;
        this.adRanker = adRanker;
        this.adFilter = adFilter;
        this.adCampaignManager = adCampaignManager;
        this.adPricing = adPricing;
    }

    public boolean preloadAds(int cacheId) {
        String adFilePath = cacheId == 1 ? adFilePath1 : adFilePath2;
        File adFile = new File(getClass().getClassLoader().getResource(adFilePath).getFile());

        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(adFile))) {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                JSONObject adObject = new JSONObject(line);
                Ad ad = new Ad();
                if (adObject.isNull("adId") || adObject.isNull("campaignId")) {
                    continue;
                }
                ad.adId = adObject.getLong(AD_ID);
                ad.campaignId = adObject.getLong(CAMPAIGN_ID);
                ad.brand = adObject.isNull(BRAND) ? "" : adObject.getString(BRAND);
                ad.price = adObject.isNull(PRICE) ? 100.0 : adObject.getDouble(PRICE);
                ad.thumbnail = adObject.isNull(THUMBNAIL) ? "" : adObject.getString(THUMBNAIL);
                ad.title = adObject.isNull(TITLE) ? "" : adObject.getString(TITLE);
                ad.detailUrl = adObject.isNull(DETAIL_URL) ? "" : adObject.getString(DETAIL_URL);
                ad.bidPrice = adObject.isNull(BID_PRICE) ? 1.0 : adObject.getDouble(BID_PRICE);
                ad.pClick = adObject.isNull(P_CLICK) ? 0.0 : adObject.getDouble(P_CLICK);
                ad.category = adObject.isNull(CATEGORY) ? "" : adObject.getString(CATEGORY);
                ad.description = adObject.isNull(DESCRIPTION) ? "" : adObject.getString(DESCRIPTION);
                List<String> keyWords = new ArrayList<>();
                JSONArray keyWordsObj = adObject.isNull(KEY_WORDS) ? null : adObject.getJSONArray(KEY_WORDS);
                if (keyWordsObj != null) {
                    for (int i = 0; i < keyWordsObj.length(); i++) {
                        keyWords.add(keyWordsObj.getString(i));
                    }
                }
                ad.keyWords = String.join(",", keyWords);
                adService.save(ad);
                indexBuilder.buildInvertedIndex(ad, cacheId);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return false;
    }

    public boolean preloadCampaigns() {
        File campaignFile = new File(getClass().getClassLoader().getResource(campaignFilePath).getFile());

        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(campaignFile))) {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                JSONObject adObject = new JSONObject(line);
                Campaign campaign = new Campaign();
                if (adObject.isNull(BUDGET) || adObject.isNull(CAMPAIGN_ID)) {
                    continue;
                }
                campaign.setBudget(adObject.getDouble(BUDGET));
                campaign.setCampaignId(adObject.getLong(CAMPAIGN_ID));
                campaignService.saveCampaign(campaign);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return false;
    }

    public List<Ad> selectAds(String query, String deviceId, String deviceIp, String queryCategory) {
        List<Ad> level0Ads = new ArrayList<>();

        List<List<String>> rewrittenQueries = queryParser.queryRewrite(query);
        Set<Long> uniqueAds = new HashSet<>();
        for (List<String> queryTerms : rewrittenQueries) {
            AdSelectResult adSelectResult = getAdsFromIndexServer(queryTerms);
            log.info("Number of level0Ads from index server = " + adSelectResult.getAdList().size());

            for (com.bihju.adindex.Ad adindexAd : adSelectResult.getAdList()) {
                log.info("Relevance score = " + adindexAd.getRelevanceScore());

                if (!uniqueAds.contains(adindexAd.getAdId())) {
                    uniqueAds.add(adindexAd.getAdId());
                    Ad ad = adConverter.cloneAd(adindexAd);
                    level0Ads.add(ad);
                }
            }

            log.info("Number of L0 level0Ads = " + level0Ads.size());
        }

        List<Ad> rankedAds = adRanker.rankAds(level0Ads);
        log.info("After ranking, ads size = " + rankedAds.size());

        int k = 20;
        List<Ad> level1Ads = adFilter.filterAds(rankedAds, k);
        log.info("After filtering, ads size = " + level1Ads.size());

        List<Ad> deDupedAds = adCampaignManager.deDupeAdsByCampaignId(level1Ads);
        log.info("After de-duping, ads size = " + deDupedAds.size());

        adPricing.setCostPerClick(deDupedAds);
        List<Ad> finalAds = adCampaignManager.applyBudget(deDupedAds);
        log.info("Final ads count = " + finalAds.size());

        return finalAds;
    }

    public AdSelectResult getAdsFromIndexServer(List<String> queryTerms) {
        AdSelectResult adSelectResult = new AdSelectResult();
        Query.Builder queryBuilder = Query.newBuilder();
        for (String queryTerm : queryTerms) {
            log.info("queryTerm = " + queryTerm);
            queryBuilder.addTerm(queryTerm);
        }
        log.info("Query term count = " + queryBuilder.getTermCount());

        List<Query> queryList = new ArrayList<>();
        queryList.add(queryBuilder.build());

        IndexClientWorker indexClientWorker1 = new IndexClientWorker(queryList, server1, port1, adSelectResult);
        IndexClientWorker indexClientWorker2 = new IndexClientWorker(queryList, server2, port2, adSelectResult);
        indexClientWorker1.start();
        indexClientWorker2.start();

        try {
            indexClientWorker1.join(INDEX_SERVER_TIMEOUT);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        try {
            indexClientWorker2.join(INDEX_SERVER_TIMEOUT);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return adSelectResult;
    }
}

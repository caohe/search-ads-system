package com.bihju.utility;

import com.bihju.adindex.Query;
import com.bihju.domain.Ad;
import com.bihju.domain.Campaign;
import com.bihju.service.AdService;
import com.bihju.service.CampaignService;
import com.google.common.base.Strings;
import lombok.extern.log4j.Log4j;
import net.spy.memcached.AddrUtil;
import net.spy.memcached.ConnectionFactoryBuilder;
import net.spy.memcached.FailureMode;
import net.spy.memcached.MemcachedClient;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Random;

@Log4j
@Component
public class AdEngine {
    private static final String AD_ID = "adId";
    private static final String CAMPAIGN_ID = "campaignId";
    private static final String BRAND = "brand";
    private static final String PRICE = "price";
    private static final String THUMBNAIL = "thumbnail";
    private static final String TITLE = "title";
    private static final String DETAIL_URL = "detail_url";
    private static final String BID_PRICE = "bidPrice";
    private static final String P_CLICK = "pClick";
    private static final String CATEGORY = "category";
    private static final String DESCRIPTION = "description";
    private static final String KEY_WORDS = "keyWords";
    private static final String BUDGET = "budget";
    private static final int INDEX_SERVER_TIMEOUT = 1000;
    private static final String DEVICE_IP_CLICK_PREFIX = "dipc_";
    private static final String DEVICE_IP_IMPRESSION_PREFIX = "dipi_";
    private static final String DEVICE_ID_CLICK_PREFIX = "didc_";
    private static final String DEVICE_ID_IMPRESSION_PREFIX = "didi_";
    private static final String AD_ID_CLICK_PREFIX = "aidc_";
    private static final String AD_ID_IMPRESSION_PREFIX = "aidi_";
    private static final String QUERY_CAMPAIGN_ID_CLICK_PREFIX = "qcidc_";
    private static final String QUERY_CAMPAIGN_ID_IMPRESSION_PREFIX = "qcidi_";
    private static final String QUERY_AD_ID_CLICK_PREFIX = "qaidc_";
    private static final String QUERY_AD_ID_IMPRESSION_PREFIX = "qaidi_";

    private IndexBuilder indexBuilder;
    private AdService adService;
    private CampaignService campaignService;
    private QueryParser queryParser;
    private AdConverter adConverter;
    private AdRanker adRanker;
    private AdFilter adFilter;
    private AdCampaignManager adCampaignManager;
    private AdPricing adPricing;
    private AdAllocation adAllocation;
    private MemcachedClient featureCacheClient;
    private CTRModel cTrModel;
    private boolean isEnablePClick = true;
    private ResourceLoader resourceLoader;
    private File synonymFile;
    private MemcachedClient synCache;
    private final static int EXP = 72000; // 0: never expire

    @Value("${index.server1.address}")
    private String server1;
    @Value("${index.server2.address}")
    private String server2;
    @Value("${index.server1.port}")
    private int port1;
    @Value("${index.server2.port}")
    private int port2;
    @Value("classpath:${ad_file_path1}")
    private String adFilePath1;
    @Value("classpath:${ad_file_path2}")
    private String adFilePath2;
    @Value("classpath:${campaign_file_path}")
    private String campaignFilePath;
    @Value("classpath:${synonym_file_path}")
    private String synonymFilePath;

    @Autowired
    public AdEngine(IndexBuilder indexBuilder, AdService adService, CampaignService campaignService,
                    QueryParser queryParser, AdConverter adConverter, AdRanker adRanker, AdFilter adFilter,
                    AdCampaignManager adCampaignManager, AdPricing adPricing, AdAllocation adAllocation,
                    @Value("${cache.server}") String cacheServer, @Value("${cache.feature_port}") int featurePort,
                    CTRModel cTrModel, @Value("${cache.syn_port}") int synPort, ResourceLoader resourceLoader) {
        this.indexBuilder = indexBuilder;
        this.adService = adService;
        this.campaignService = campaignService;
        this.queryParser = queryParser;
        this.adConverter = adConverter;
        this.adRanker = adRanker;
        this.adFilter = adFilter;
        this.adCampaignManager = adCampaignManager;
        this.adPricing = adPricing;
        this.adAllocation = adAllocation;
        if (!Strings.isNullOrEmpty(cacheServer)) {
            this.featureCacheClient = getMemCachedClient(cacheServer + ":" + featurePort);
            this.synCache = getMemCachedClient(cacheServer + ":" + synPort);
        }
        this.cTrModel = cTrModel;
        this.resourceLoader = resourceLoader;

    }

    public boolean preloadAds(int cacheId) {
        String adFilePath = cacheId == 1 ? adFilePath1 : adFilePath2;
//        File adFile = new File(getClass().getClassLoader().getResource(adFilePath).getFile());

        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(resourceLoader.getResource(adFilePath).getInputStream()))) {
//        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(adFile))) {
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
//        File campaignFile = new File(getClass().getClassLoader().getResource(campaignFilePath).getFile());
        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(resourceLoader.getResource(campaignFilePath).getInputStream()))) {
//        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(campaignFile))) {
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

    public List<Ad> selectAds(String query, String deviceIp, String deviceId, String queryCategory) {
        List<Ad> level0Ads = new ArrayList<>();

        boolean enable_query_rewrite = false; // turn off for now
        if (enable_query_rewrite) {
            List<List<String>> rewrittenQueries = queryParser.queryRewrite(query);
            Set<Long> uniqueAds = new HashSet<>();
            for (List<String> queryTerms : rewrittenQueries) {
                AdSelectResult adSelectResult = getAdsFromIndexServer(queryTerms);
                log.info("Number of level0Ads from index server = " + adSelectResult.getAdList().size());

                for (com.bihju.adindex.Ad adindexAd : adSelectResult.getAdList()) {
                    if (!uniqueAds.contains(adindexAd.getAdId())) {
                        uniqueAds.add(adindexAd.getAdId());
                        Ad ad = adConverter.cloneAd(adindexAd);
                        level0Ads.add(ad);
                    }
                }
            }
        } else {
            List<String> queryTerms = Utility.cleanedTokenize(query);
            AdSelectResult adSelectResult = getAdsFromIndexServer(queryTerms);
            log.info("Number of level0Ads from index server = " + adSelectResult.getAdList().size());

            for(com.bihju.adindex.Ad adindexAd : adSelectResult.getAdList()) {
                Ad ad = adConverter.cloneAd(adindexAd);
                level0Ads.add(ad);
            }
        }

        if (isEnablePClick) {
            for(Ad ad : level0Ads) {
                predictCTR(ad, query.replace(" ", "_"), deviceIp, deviceId, queryCategory);
            }
        }

        log.info("Number of L0 level0Ads = " + level0Ads.size());
        List<Ad> rankedAds = adRanker.rankAds(level0Ads);
        log.info("After ranking, ads size = " + rankedAds.size());

        int k = 20;
        List<Ad> level1Ads = adFilter.filterAds(rankedAds, k);
        log.info("After filtering, ads size = " + level1Ads.size());

        List<Ad> deDupedAds = adCampaignManager.deDupeAdsByCampaignId(level1Ads);
        log.info("After de-duping, ads size = " + deDupedAds.size());



        adPricing.setCostPerClick(deDupedAds);
        // for (Ad ad : deDupedAds) {
        //     log.info("ad.costPerClick = " + ad.getCostPerClick());
        //     // ad.qualityScore = d * ad.pClick + (1.0 - d) * ad.relevanceScore;
        //     // ad.rankScore = ad.qualityScore * ad.bidPrice;
        // }
        List<Ad> finalAds = adCampaignManager.applyBudget(deDupedAds);

        // for (Ad ad : finalAds) {
        //     log.info("ad.pclick = " + ad.pClick);
        //     // ad.qualityScore = d * ad.pClick + (1.0 - d) * ad.relevanceScore;
        //     // ad.rankScore = ad.qualityScore * ad.bidPrice;
        // }

        log.info("Final ads count = " + finalAds.size());

        adAllocation.allocateAds(finalAds);



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

    private MemcachedClient getMemCachedClient(String cacheAddress) {
        try {
            return new MemcachedClient(
                    new ConnectionFactoryBuilder()
                            .setDaemon(true)
                            .setFailureMode(FailureMode.Retry)
                            .build(), AddrUtil.getAddresses(cacheAddress));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void predictCTR(Ad ad, String query, String deviceId, String deviceIp, String queryCategory) {
        // Features has to be on the same order as the one used for training.
        Random random = new Random();

        ArrayList<Double> features = new ArrayList<>();

        features.add(getFeatureValue(DEVICE_IP_CLICK_PREFIX + deviceIp));
        features.add(getFeatureValue(DEVICE_IP_IMPRESSION_PREFIX + deviceIp));

        features.add(getFeatureValue(DEVICE_ID_CLICK_PREFIX + deviceId));
        features.add(getFeatureValue(DEVICE_ID_IMPRESSION_PREFIX + deviceId));

        features.add(getFeatureValue(AD_ID_CLICK_PREFIX + ad.adId) == 0 ? (random.nextInt(73)%(73-35+1) + 35) :getFeatureValue(AD_ID_CLICK_PREFIX + ad.adId) );
        features.add(getFeatureValue(AD_ID_IMPRESSION_PREFIX + ad.adId)== 0 ? (random.nextInt(73)%(73-35+1) + 35) :getFeatureValue(AD_ID_IMPRESSION_PREFIX + ad.adId) );

        features.add(getFeatureValue(QUERY_CAMPAIGN_ID_CLICK_PREFIX +  ad.campaignId) == 0 ? (random.nextInt(360)%(360-120+1) + 120):getFeatureValue(QUERY_CAMPAIGN_ID_CLICK_PREFIX +  ad.campaignId));
        features.add(getFeatureValue(QUERY_CAMPAIGN_ID_IMPRESSION_PREFIX + ad.campaignId) == 0 ? (random.nextInt(360)%(360-120+1) + 120):getFeatureValue(QUERY_CAMPAIGN_ID_IMPRESSION_PREFIX + ad.campaignId));

        features.add(getFeatureValue(QUERY_AD_ID_CLICK_PREFIX + query + "_" + ad.adId)== 0 ? (random.nextInt(73)%(73-35+1) + 35) :getFeatureValue(QUERY_AD_ID_CLICK_PREFIX + query + "_" + ad.adId));
        features.add(getFeatureValue(QUERY_AD_ID_IMPRESSION_PREFIX + query + "_" + ad.adId)== 0 ? (random.nextInt(73)%(73-35+1) + 35) :getFeatureValue(QUERY_AD_ID_IMPRESSION_PREFIX + query + "_" + ad.adId) );

        // Set to a large number if matches.
        features.add(queryCategory.equals(ad.category) ? 1000000.0 : 0.0);

        // cTrModel.loadWeights();
        ad.pClick = cTrModel.predictCTRWithLogisticRegression(features);
        log.info("ad.pClick = " + ad.pClick);

      }
    private double getFeatureValue(String key) {
        // log.info("Key = " + key);
        double value = 0.0;
        @SuppressWarnings("unchecked")
        String valueString = (String)featureCacheClient.get(key);
        // log.info(valueString);
        if (!Strings.isNullOrEmpty(valueString)) {
            value = Double.parseDouble(valueString);
        }

        // log.info("Value = " + value);
        return value;
    }

    public boolean preloadSynonyms() {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(resourceLoader.getResource(synonymFilePath).getInputStream()))) {
            String line ;
            JSONArray synonymsObj = null;
            while ((line = br.readLine()) != null) {
                JSONObject synonymObject = new JSONObject(line);
                String key = synonymObject.getString("word");
                    synonymsObj = synonymObject.getJSONArray("synonyms");
                    List<String> synonyms = new ArrayList<String>();
                    for(int i = 0; i < synonymsObj.length();i++) {
                        synonyms.add(synonymsObj.getString(i));
                    }
                    synCache.set(key, EXP, synonyms);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return false;
    }

}

package com.hecao.util;

import com.hecao.service.AdService;
import com.hecao.adindex.Ad;
import com.hecao.adindex.Query;
import lombok.extern.log4j.Log4j;
import net.spy.memcached.AddrUtil;
import net.spy.memcached.ConnectionFactoryBuilder;
import net.spy.memcached.FailureMode;
import net.spy.memcached.MemcachedClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;

@Component
@Log4j
public class AdsSelector {
    private AdService adService;
    private final int NUM_OF_DOCS = 10840;
    private Boolean enableTFIDF = false;

    private MemcachedClient indexCacheClient;
    private MemcachedClient tfCacheClient;
    private MemcachedClient dfCacheClient;

    @Autowired
    public AdsSelector(AdService adService, @Value("${cache.server}") String cacheServer,
                       @Value("${cache.index_port}") int indexPort,
                       @Value("${cache.tf_port}") int tfPort,
                       @Value("${cache.df_port}") int dfPort) {
        this.adService = adService;
        indexCacheClient = getMemCachedClient(cacheServer + ":" + indexPort);
        tfCacheClient = getMemCachedClient(cacheServer + ":" + tfPort);
        dfCacheClient = getMemCachedClient(cacheServer + ":" + dfPort);
    }

    public List<Ad> selectAds(Query query) {
        List<Ad> adList = new ArrayList<>();
        Map<Long, Integer> matchedAds = new HashMap<>();
        try {
            for (String queryTerm : query.getTermList()) {
                log.info("queryTerm = " + queryTerm);
                Set<Long> adIdList = (Set<Long>) indexCacheClient.get(queryTerm);
                if (adIdList != null && adIdList.size() > 0) {
                    for (Long adId : adIdList) {
                        if (matchedAds.containsKey(adId)) {
                            int count = matchedAds.get(adId) + 1;
                            matchedAds.put(adId, count);
                        } else {
                            matchedAds.put(adId, 1);
                        }
                    }
                }
            }

            for (Long adId : matchedAds.keySet()) {
                log.info("adId = " + adId);
                Ad.Builder adBuilder = adService.getAd(adId);
                double relevanceScore = matchedAds.get(adId) * 1.0 / adBuilder.getKeyWordsList().size();
                double relevanceScoreTFIDF = getRelevanceScoreByTFIDF(
                        adId, adBuilder.getKeyWordsList().size(), query.getTermList());
                adBuilder.setRelevanceScore(enableTFIDF ? relevanceScoreTFIDF : relevanceScore);
                log.info("RelevanceScore = " + adBuilder.getRelevanceScore());
                log.info("pClick = " + adBuilder.getPClick());
                adList.add(adBuilder.build());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return adList;
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

    private double getRelevanceScoreByTFIDF(long adId, int numOfKeywords, List<String> queryTerms) {
        double relevanceScore = 0.0;
        for (String queryTerm : queryTerms) {
            relevanceScore += calculateTFIDF(adId, queryTerm, numOfKeywords);
        }

        return relevanceScore;
    }

    // TFIDF = log(numDocs / (docFreq + 1)) * sqrt(tf) * (1 / sqrt(docLength))
    private double calculateTFIDF(Long adId, String term, int docLength) {
        String tfKey = adId.toString() + "_" + term;
        log.info("tfKey = " + tfKey + ", dfKey = " + term);

        String tf = (String) tfCacheClient.get(tfKey);
        log.info("tf = " + tf == null ? "" : tf);

        String df = (String) dfCacheClient.get(term);
        log.info("df = " + df == null ? "" : df);

        if (tf != null && df != null) {
            int tfVal = Integer.parseInt(tf);
            int dfVal = Integer.parseInt(df);
            double dfScore = Math.log10(NUM_OF_DOCS * 1.0 / (dfVal + 1));
            double tfScore = Math.sqrt(tfVal);
            double norm = Math.sqrt(docLength);
            return (dfScore * tfScore) / norm;
        }

        return 0.0;
    }
}

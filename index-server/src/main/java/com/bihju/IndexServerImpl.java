package com.bihju;

import com.bihju.adindex.*;
import io.grpc.stub.StreamObserver;
import lombok.extern.log4j.Log4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Log4j
@Component
public class IndexServerImpl extends AdsIndexGrpc.AdsIndexImplBase {
    private final static double RELEVANCE_SCORE_THRESHOLD = 0.07;
    private AdsSelector adsSelector;
    private int cacheId;

    @Autowired
    public IndexServerImpl(AdsSelector adsSelector) {
        this.adsSelector = adsSelector;
    }

    public void setCacheId(int cacheId) {
        this.cacheId = cacheId;
    }

    @Override
    public void getAds(AdsRequest adsRequest, StreamObserver<AdsReply> response) {
        log.info("Received requests, queryCount = " + adsRequest.getQueryCount());

        for (int i = 0; i < adsRequest.getQueryCount(); i++) {
            Query query = adsRequest.getQuery(i);

            List<Ad> adsCandidates = adsSelector.selectAds(query, cacheId);
            AdsReply.Builder replyBuilder = AdsReply.newBuilder();
            for (Ad ad : adsCandidates) {
                if (ad.getRelevanceScore() > RELEVANCE_SCORE_THRESHOLD) {
                    replyBuilder.addAd(ad);
                }
            }

            AdsReply reply = replyBuilder.build();
            response.onNext(reply);
            response.onCompleted();
        }
    }
}

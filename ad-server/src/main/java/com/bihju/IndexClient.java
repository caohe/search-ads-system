package com.bihju;

import com.bihju.adindex.Ad;
import com.bihju.adindex.AdsIndexGrpc;
import com.bihju.adindex.AdsReply;
import com.bihju.adindex.AdsRequest;
import com.bihju.adindex.Query;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.extern.log4j.Log4j;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Log4j
public class IndexClient {
    private ManagedChannel managedChannel;
    private AdsIndexGrpc.AdsIndexBlockingStub adsIndexBlockingStub;

    public IndexClient(String server, int port) {
        this.managedChannel = ManagedChannelBuilder.forAddress(server, port)
                // disable TLS to avoid certification error.
                .usePlaintext(true).build();
        this.adsIndexBlockingStub = AdsIndexGrpc.newBlockingStub(managedChannel);
    }

    public void shutdown() throws InterruptedException {
        managedChannel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

    public List<Ad> getAds(List<Query> queries) {
        AdsRequest.Builder adsRequestBuilder = AdsRequest.newBuilder();
        log.info("queries size = " + queries.size());

        for (Query query : queries) {
            log.info("Query term count = " + query.getTermCount());
            for (int i = 0; i < query.getTermCount(); i++) {
                log.info("term = " + query.getTerm(i));
            }
            adsRequestBuilder.addQuery(query);
        }

        AdsReply adsReply;
        log.info("Sending request...");
        adsReply = adsIndexBlockingStub.getAds(adsRequestBuilder.build());
        return adsReply.getAdList();
    }
}

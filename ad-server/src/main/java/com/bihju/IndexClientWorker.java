package com.bihju;

import com.bihju.adindex.Ad;
import com.bihju.adindex.Query;

import java.util.List;

public class IndexClientWorker extends Thread {
    private AdSelectResult adSelectResult;
    private String indexServer;
    private int indexServerPort;
    private List<Query> queries;

    public IndexClientWorker(List<Query> queries, String indexServer, int indexServerPort,
                             AdSelectResult adSelectResult) {
        this.queries = queries;
        this.indexServer = indexServer;
        this.indexServerPort = indexServerPort;
    }

    public void start() {
        IndexClient indexClient = new IndexClient(indexServer, indexServerPort);
        // TODO Add time out to return partial list of ads.
        List<Ad> ads = indexClient.getAds(queries);
        adSelectResult.add(ads);
    }
}

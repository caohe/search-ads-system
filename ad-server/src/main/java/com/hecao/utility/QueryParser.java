package com.hecao.utility;

import net.spy.memcached.MemcachedClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.*;

@Component
public class QueryParser {
    private static final String REWRITE_PREFIX = "REWRITE_";
    private final int EXP = 72000; //0: never expire
    private String synonymFilePath;
    private File synonymFile;
    private MemcachedClient synonymCache;
    private MemcachedClient synCache;
    private ResourceLoader resourceLoader;

    @Autowired
    public QueryParser(@Value("classpath:${synonym_file_path}") String synonymFilePath,
                       @Value("${cache.server}") String cacheServer,
                       @Value("${cache.synonym_port}") int synonymPort,
                       @Value("${cache.syn_port}") int synPort,
                        ResourceLoader resourceLoader) {
        this.synonymFilePath = synonymFilePath;
        synonymCache = Utility.getMemCachedClient(cacheServer + ":" + synonymPort);
        synCache = Utility.getMemCachedClient(cacheServer + ":" + synPort);
//        this.synonymFile = new File(getClass().getClassLoader().getResource(synonymFilePath).getFile());
        this.resourceLoader = resourceLoader;
    }

    public List<List<String>> queryRewrite(String query) {
        String cleanedQuery = Utility.cleanedQuery(query);
        List<String> rewrittenQueries = getRewrittenQueriesFromCache(cleanedQuery);
        if (rewrittenQueries == null || rewrittenQueries.isEmpty()) {
            rewrittenQueries = calculateRewrittenQueries(cleanedQuery);
        }
        List<List<String>> queryTerms = new ArrayList<>();
        if (rewrittenQueries == null) {
            queryTerms.add(Utility.cleanedTokenize(query));
        } else {
            for (String rewrittenQuery : rewrittenQueries) {
                queryTerms.add(Utility.cleanedTokenize(rewrittenQuery));
            }
        }

        return queryTerms;
    }

    private List<String> getRewrittenQueriesFromCache(String cleanedQuery) {
        return (List<String>) synonymCache.get((REWRITE_PREFIX + cleanedQuery).replace(" ", "_"));
    }

    private void setRewrittenQueriesToCache(String query, List<String> rewrittenQueries) {
        synonymCache.set((REWRITE_PREFIX + query).replace(" ", "_"), EXP, rewrittenQueries);
    }

    private List<String> calculateRewrittenQueries(String query) {
        List<String> searchKeys = Arrays.asList(query.split(" "));
        Map<String, List<String>> map = new HashMap<>();

        for(String key:searchKeys){
            List<String> synonyms = (List<String>)synCache.get(key);
            map.put(key, synonyms);
        }

        List<String> rewrittenQueries = Utility.getRewrittenQueries(query, map);
        this.setRewrittenQueriesToCache(query, rewrittenQueries);
        return rewrittenQueries;
    }
}

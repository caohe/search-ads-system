package com.bihju;

import net.spy.memcached.MemcachedClient;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

@Component
public class QueryParser {
    private static final String REWRITE_PREFIX = "REWRITE_";
    private final int EXP = 72000; //0: never expire

    public List<List<String>> queryRewrite(String query, String memCachedServer, int memCachedPortal, String synonymsFilePath) {
        String cleanedQuery = Utility.cleanedQuery(query);
        MemcachedClient cache = Utility.getMemCachedClient(memCachedServer + ":" + memCachedPortal);
        List<String> rewrittenQueries = getRewrittenQueriesFromCache(cleanedQuery, cache);
        if (rewrittenQueries == null || rewrittenQueries.isEmpty()) {
            rewrittenQueries = calculateRewrittenQueries(cleanedQuery, synonymsFilePath, cache);
        }
        List<List<String>> queryTerms = new ArrayList<>();
        for (String rewrittenQuery : rewrittenQueries) {
            queryTerms.add(Utility.cleanedTokenize(rewrittenQuery));
        }
        return queryTerms;
    }

    private List<String> getRewrittenQueriesFromCache(String cleanedQuery, MemcachedClient cache) {
        return (List<String>)cache.get((REWRITE_PREFIX + cleanedQuery).replace(" ", "_"));
    }

    private void setRewrittenQueriesToCache(String query, List<String> rewrittenQueries, MemcachedClient cache) {
        cache.set((REWRITE_PREFIX + query).replace(" ", "_"), EXP, rewrittenQueries);
    }

    private List<String> calculateRewrittenQueries(String query, String mSynonymsFilePath, MemcachedClient cache) {
        List<String> searchKeys = Arrays.asList(query.split(" "));
        Map<String, List<String>> map = new HashMap<>();

        try (BufferedReader br = new BufferedReader(new FileReader(mSynonymsFilePath))) {
            String line ;
            JSONArray synonymsObj = null;
            while ((line = br.readLine()) != null) {
                JSONObject synonymObject = new JSONObject(line);

                String key = synonymObject.getString("word");
                if (searchKeys.contains(key)) {
                    synonymsObj = synonymObject.getJSONArray("synonyms");
                    List<String> synonyms = new ArrayList<String>();
                    for(int i = 0; i < synonymsObj.length();i++) {
                        synonyms.add(synonymsObj.getString(i));
                    }
                    map.put(key, synonyms);
                }
            }

            List<String> rewrittenQueries = Utility.getRewrittenQueries(query, map);
            this.setRewrittenQueriesToCache(query, rewrittenQueries, cache);
            return rewrittenQueries;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }
}

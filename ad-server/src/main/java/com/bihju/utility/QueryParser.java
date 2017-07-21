package com.bihju.utility;

import net.spy.memcached.MemcachedClient;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

    @Autowired
    public QueryParser(@Value("${synonym_file_path}") String synonymFilePath,
                       @Value("${cache.server}") String cacheServer,
                       @Value("${cache.synonym_port}") int synonymPort) {
        this.synonymFilePath = synonymFilePath;
        synonymCache = Utility.getMemCachedClient(cacheServer + ":" + synonymPort);
        this.synonymFile = new File(getClass().getClassLoader().getResource(synonymFilePath).getFile());
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

    // TODO Load synonyms to cache for faster access
    private List<String> calculateRewrittenQueries(String query) {
        List<String> searchKeys = Arrays.asList(query.split(" "));
        Map<String, List<String>> map = new HashMap<>();

        try (BufferedReader br = new BufferedReader(new FileReader(synonymFile))) {
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
            this.setRewrittenQueriesToCache(query, rewrittenQueries);
            return rewrittenQueries;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }
}

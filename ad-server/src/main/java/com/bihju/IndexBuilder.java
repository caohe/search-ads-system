package com.bihju;

import com.bihju.domain.Ad;
import net.spy.memcached.MemcachedClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class IndexBuilder {
    private final static int EXP = 72000; // 0: never expire

    private MemcachedClient adCache;

    public IndexBuilder(@Value("${cache.server}") String cacheServer, @Value("${cache.ad_port}") int adPort) {
        try {
            adCache = new MemcachedClient(new InetSocketAddress(cacheServer, adPort));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean buildInvertedIndex(Ad ad) {
        String keyWords = ad.getKeyWords();
        List<String> tokens = Utility.cleanedTokenize(keyWords);
        for (String token : tokens) {
            if (adCache.get(token) instanceof Set) {
                Set<Long> adIdList = (Set<Long>) adCache.get(token);
                adIdList.add(ad.getAdId());
                adCache.set(token, EXP, adIdList);
            } else {
                Set<Long> adIdList = new HashSet<>();
                adIdList.add(ad.getAdId());
                adCache.set(token, EXP, adIdList);
            }
        }

        return false;
    }
}

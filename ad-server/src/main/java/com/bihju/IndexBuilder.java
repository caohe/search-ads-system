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

    private MemcachedClient indexCache1;
    private MemcachedClient indexCache2;

    public IndexBuilder(@Value("${cache.server}") String cacheServer, @Value("${cache.index_port1}") int indexPort1,
                        @Value("${cache.index_port2}") int indexPort2) {
        try {
            indexCache1 = new MemcachedClient(new InetSocketAddress(cacheServer, indexPort1));
            indexCache2 = new MemcachedClient(new InetSocketAddress(cacheServer, indexPort2));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean buildInvertedIndex(Ad ad, int cacheId) {
        MemcachedClient indexCache = cacheId == 1 ? indexCache1 : indexCache2;

        String keyWords = ad.getKeyWords();
        List<String> tokens = Utility.cleanedTokenize(keyWords);
        for (String token : tokens) {
            if (indexCache.get(token) instanceof Set) {
                Set<Long> adIdList = (Set<Long>) indexCache.get(token);
                adIdList.add(ad.getAdId());
                indexCache.set(token, EXP, adIdList);
            } else {
                Set<Long> adIdList = new HashSet<>();
                adIdList.add(ad.getAdId());
                indexCache.set(token, EXP, adIdList);
            }
        }

        return false;
    }
}

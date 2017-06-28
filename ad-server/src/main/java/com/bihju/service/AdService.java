package com.bihju.service;

import com.bihju.domain.Ad;
import org.springframework.stereotype.Service;

@Service
public interface AdService {
    void save(Ad ad);
    Ad getAd(long adId);
}

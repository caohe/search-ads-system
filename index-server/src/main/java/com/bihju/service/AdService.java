package com.bihju.service;

import com.bihju.adindex.Ad;
import org.springframework.stereotype.Service;

@Service
public interface AdService {
    Ad.Builder getAd(long adId);
}

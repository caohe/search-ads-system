package com.hecao.service;

import com.hecao.adindex.Ad;
import org.springframework.stereotype.Service;

@Service
public interface AdService {
    Ad.Builder getAd(long adId);
}

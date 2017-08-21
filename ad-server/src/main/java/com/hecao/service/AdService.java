package com.hecao.service;

import com.hecao.domain.Ad;
import org.springframework.stereotype.Service;

@Service
public interface AdService {
    void save(Ad ad);
    Ad getAd(long adId);
}

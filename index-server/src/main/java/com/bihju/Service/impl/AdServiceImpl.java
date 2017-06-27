package com.bihju.Service.impl;

import com.bihju.Service.AdService;
import com.bihju.adindex.Ad;
import com.bihju.repository.AdRepository;
import com.bihju.util.AdConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AdServiceImpl implements AdService {
    private AdRepository adRepository;

    @Autowired
    public AdServiceImpl(AdRepository adRepository) {
        this.adRepository = adRepository;
    }

    @Override
    public Ad.Builder getAd(long adId) {
        com.bihju.domain.Ad domainAd = adRepository.findOne(adId);
        return AdConverter.cloneAd(domainAd);

    }
}

package com.hecao.service.impl;

import com.hecao.service.AdService;
import com.hecao.adindex.Ad;
import com.hecao.repository.AdRepository;
import com.hecao.util.AdConverter;
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
        com.hecao.domain.Ad domainAd = adRepository.findByAdId(adId);
        return AdConverter.cloneAd(domainAd);

    }
}

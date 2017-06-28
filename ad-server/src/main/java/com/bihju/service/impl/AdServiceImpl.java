package com.bihju.service.impl;

import com.bihju.domain.Ad;
import com.bihju.repository.AdRepository;
import com.bihju.service.AdService;
import lombok.extern.log4j.Log4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Log4j
@Service
public class AdServiceImpl implements AdService {
    private AdRepository adRepository;

    @Value("${cache.server}")
    private String cacheSever;

    @Value("${cache.synonym_port}")
    private int synonymPort;

    @Value("${synonym_file_path}")
    private String synonymFilePath;

    @Autowired
    public AdServiceImpl(AdRepository adRepository) {
        this.adRepository = adRepository;
    }

    @Override
    public void save(Ad ad) {
        Ad foundAd = adRepository.findByAdId(ad.getAdId());
        if (foundAd == null) {
            adRepository.save(ad);
        }
    }

    @Override
    public Ad getAd(long adId) {
        return adRepository.getOne(adId);
    }
}

package com.hecao.service.impl;

import com.hecao.domain.Ad;
import com.hecao.repository.AdRepository;
import com.hecao.service.AdService;
import lombok.extern.log4j.Log4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Random;

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
        } else {
            Random random = new Random();
            foundAd.setCostPerClick(random.nextDouble());
            adRepository.save(foundAd);
        }
    }

    @Override
    public Ad getAd(long adId) {
        return adRepository.findByAdId(adId);
    }
}

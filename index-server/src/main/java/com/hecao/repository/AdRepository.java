package com.hecao.repository;

import com.hecao.domain.Ad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AdRepository extends JpaRepository<Ad, Long> {
    Ad findByAdId(long adId);
}

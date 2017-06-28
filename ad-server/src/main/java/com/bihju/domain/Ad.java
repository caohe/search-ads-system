package com.bihju.domain;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.annotation.PersistenceConstructor;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Data
@RequiredArgsConstructor(onConstructor = @__(@PersistenceConstructor))
@Table(name = "ad")
public class Ad {
    @Id
    @GeneratedValue
    private Long id;

    public Long adId;
    public Long campaignId;
    public String keyWords;
    public double relevanceScore;
    public double pClick;
    public double bidPrice;
    public double rankScore;
    public double qualityScore;
    public double costPerClick;
    public int position;//1: top , 2: bottom
    public String title; // required
    public double price; // required
    public String thumbnail; // required
    public String description; // required
    public String brand; // required
    public String detailUrl; // required
    public String query; //required
    public String category;

}

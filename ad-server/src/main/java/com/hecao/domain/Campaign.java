package com.hecao.domain;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Entity
@Data
public class Campaign {
    @Id
    @GeneratedValue
    private Long id;

    private Long campaignId;
    private double budget;
}

package com.bihju;

import com.bihju.adindex.Ad;

import java.util.ArrayList;
import java.util.List;

public class AdSelectResult {
    private List<Ad> adList;

    public AdSelectResult() {
        adList = new ArrayList<>();
    }

    public synchronized void add(List<Ad> adList) {
        if (adList == null) {
            return;
        }

        for (Ad ad : adList) {
            this.adList.add(ad);
        }
    }

    public synchronized List<Ad> getAdList() {
        return adList;
    }
}

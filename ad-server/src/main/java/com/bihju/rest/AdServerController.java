package com.bihju.rest;

import com.bihju.utility.AdEngine;
import com.bihju.domain.Ad;
import lombok.extern.log4j.Log4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/ad-server")
@Log4j
public class AdServerController {
    private AdEngine adEngine;

    @Autowired
    public AdServerController(AdEngine adEngine) {
        this.adEngine = adEngine;
    }

    @RequestMapping(value = "version", method = RequestMethod.GET)
    public String getVersion() {
        return "1.0.0";
    }

    @RequestMapping(value = "ads/{query}", method = RequestMethod.GET)
    public List<Ad> searchAds(@PathVariable String query, HttpServletRequest request,
                              HttpServletResponse response) throws IOException {
        String deviceId = request.getParameter("did");
        String deviceIp = request.getParameter("dip");
        String queryCategory = request.getParameter("qclass");

        return adEngine.selectAds(query, deviceIp, deviceId, queryCategory);
    }

    @RequestMapping(value = "ads", method = RequestMethod.GET)
    public String preloadAds() {
        adEngine.preloadAds(1);
        adEngine.preloadAds(2);
        adEngine.preloadCampaigns();
        return "Success";
    }
}

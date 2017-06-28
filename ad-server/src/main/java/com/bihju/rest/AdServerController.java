package com.bihju.rest;

import com.bihju.AdEngine;
import com.bihju.domain.Ad;
import lombok.extern.log4j.Log4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/ad-server")
@Log4j
public class AdServerController {
    private AdEngine adEngine;

    @Value("${ui_template_file_path")
    private String uiTemplate;
    @Value("${ad_template_file_path")
    private String adTemplate;

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

        List<Ad> ads = adEngine.selectAds(query, deviceId, deviceIp, queryCategory);
//        String result = uiTemplate;
        StringBuilder sb = new StringBuilder();
        for (Ad ad : ads) {
            log.info("Ad id = " + ad.getAdId());
            log.info("Ad rank score = " + ad.getRankScore());
            String adContent = adTemplate
                    .replace("$title$", ad.title)
                    .replace("brands$", ad.getBrand())
                    .replace("$img$", ad.getThumbnail())
                    .replace("$link$", ad.getDetailUrl())
                    .replace("$price$", Double.toString(ad.getPrice()));
            sb.append(adContent);
        }

//        result = result.replace("$list$", sb.toString());
//        response.setContentType("text/html; charset=UTF-8");
//        response.getWriter().write(result);
        return ads;
    }

    @RequestMapping(value = "ads", method = RequestMethod.GET)
    public String preloadAds() {
        adEngine.preloadAds(1);
        adEngine.preloadAds(2);
        adEngine.preloadCampaigns();
        return "Success";
    }
}

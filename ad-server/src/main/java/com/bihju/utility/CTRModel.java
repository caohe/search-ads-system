package com.bihju.utility;

import lombok.extern.log4j.Log4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

@Log4j
@Component
public class CTRModel {
    // private static ArrayList<Double> weightsLogistic;
    // private static double biasLogistic;

    @Autowired
    public CTRModel(){
//    public CTRModel(@Value("classpath:${logistic-regressi
    }

    public double predictCTRWithLogisticRegression(ArrayList<Double> features) {
        double biasLogistic = 0;
        double[] weightsLogistic = {0, 0, 0, 0, 0.0679072624714, 0.0679072624714, 0.00184427035092, 0.00184427035092, 0.0679072624714, 0.0679072624714, 0};
        double pClick = biasLogistic;

        if (features.size() != weightsLogistic.length) {
            log.error("ERROR : features size doesn't match with weights");
            return pClick;
        }

        for (int i = 0; i < features.size(); i++) {
            // log.info(features.get(i));
            pClick = pClick + weightsLogistic[i] * features.get(i);
        }
        pClick /= 10;
        log.info("Sigmoid input pClick = " + pClick);
        pClick = Utility.sigmoid(pClick);
        return pClick;
    }

}

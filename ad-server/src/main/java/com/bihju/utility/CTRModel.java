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
    private static ArrayList<Double> weightsLogistic;
    private static Double biasLogistic;

    @Autowired
    public CTRModel(ResourceLoader resourceLoader,
                  @Value("classpath:${logistic-regression-model-file}") String logisticRegressionModelFilePath) throws IOException {
//    public CTRModel(@Value("classpath:${logistic-regression-model-file}") String logisticRegressionModelFilePath) throws IOException {
        weightsLogistic = new ArrayList<>();

//        File logisticRegressionModelFile = new File(getClass().getClassLoader().getResource(logisticRegressionModelFilePath).getFile());
//        File logisticRegressionModelFile = resourceLoader.getResource(logisticRegressionModelFilePath).getFile();

//        try (BufferedReader ctrLogisticReader = new BufferedReader(new FileReader(logisticRegressionModelFile))) {
        try (BufferedReader ctrLogisticReader = new BufferedReader(new InputStreamReader(resourceLoader.getResource(logisticRegressionModelFilePath).getInputStream()))) {
            String line;

            while ((line = ctrLogisticReader.readLine()) != null) {
                JSONObject parameterJson = new JSONObject(line);
                JSONArray weights = parameterJson.isNull("weights") ? null : parameterJson.getJSONArray("weights");
                if (weights == null) {
                    continue;
                }

                for (int j = 0; j < weights.length(); j++) {
                    weightsLogistic.add(weights.getDouble(j));
                    log.info("Weights = " + weights.getDouble(j));

                }

                biasLogistic = parameterJson.getDouble("bias");
                log.info("BiasLogistic = " + biasLogistic);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public double predictCTRWithLogisticRegression(ArrayList<Double> features) {
        double pClick = biasLogistic;

        if (features.size() != weightsLogistic.size()) {
            log.error("ERROR : features size doesn't match with weights");
            return pClick;
        }

        for (int i = 0; i < features.size(); i++) {
            pClick = pClick + weightsLogistic.get(i) * features.get(i);
        }

        log.info("Sigmoid input pClick = " + pClick);
        pClick = Utility.sigmoid(pClick);
        return pClick;
    }

}

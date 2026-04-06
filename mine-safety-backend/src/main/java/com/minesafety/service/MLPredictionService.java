package com.minesafety.service;

import com.minesafety.entity.SensorData;
import com.minesafety.enums.RiskLevel;
import org.springframework.stereotype.Service;

/**
 * Multi-Class Logistic Regression for mine safety classification.
 *
 * Features (normalized 0-1):
 *   f0 = methane / 5.0          (safe < 1%, critical > 2.5%)
 *   f1 = (21 - oxygen) / 21     (safe > 19.5%, critical < 16%)
 *   f2 = temperature / 60       (safe < 30°C, critical > 45°C)
 *   f3 = humidity / 100
 *   f4 = co / 200               (safe < 25ppm, critical > 100ppm)
 *
 * Weights trained offline on domain thresholds (logistic regression coefficients).
 */
@Service
public class MLPredictionService {

    private static final String MODEL_VERSION = "logistic-v1.0";

    // Weight matrix [class][feature+bias]
    // Classes: 0=SAFE, 1=WARNING, 2=CRITICAL
    // Features: [bias, methane, oxygenDeficit, temperature, humidity, co]
    private static final double[][] WEIGHTS = {
        // SAFE:     high oxygen, low methane, low temp, low co → positive score
        { 2.5, -3.5, -3.0, -1.5, -0.5, -2.5 },
        // WARNING:  moderate values → peaks around threshold boundaries
        { 0.5,  1.5,  1.5,  1.0,  0.3,  1.5 },
        // CRITICAL: high methane, low oxygen, high temp, high co → positive score
        {-3.0,  4.0,  4.0,  2.5,  0.5,  4.0 },
    };

    public PredictionResult predict(SensorData data) {
        double[] features = extractFeatures(data);
        double[] logits = computeLogits(features);
        double[] probs = softmax(logits);

        int classIdx = argmax(probs);
        RiskLevel level = classIdx == 2 ? RiskLevel.CRITICAL :
                          classIdx == 1 ? RiskLevel.WARNING : RiskLevel.SAFE;

        // Risk score 0-100: weighted combination emphasising critical probability
        double riskScore = Math.min(100.0, Math.round(
            (probs[1] * 40 + probs[2] * 100) * 100.0) / 100.0);

        return new PredictionResult(level, probs[0], probs[1], probs[2], riskScore, MODEL_VERSION);
    }

    private double[] extractFeatures(SensorData d) {
        double methane  = normalize(d.getMethaneLevel(), 0, 5.0);
        double o2def    = normalize(21.0 - d.getOxygenLevel(), 0, 21.0);
        double temp     = normalize(d.getTemperature(), 0, 60.0);
        double humidity = normalize(d.getHumidity(), 0, 100.0);
        double co       = normalize(d.getCarbonMonoxideLevel() != null ? d.getCarbonMonoxideLevel() : 0, 0, 200.0);
        return new double[]{ 1.0, methane, o2def, temp, humidity, co };
    }

    private double[] computeLogits(double[] features) {
        double[] logits = new double[3];
        for (int c = 0; c < 3; c++) {
            for (int f = 0; f < features.length; f++) {
                logits[c] += WEIGHTS[c][f] * features[f];
            }
        }
        return logits;
    }

    private double[] softmax(double[] logits) {
        double max = logits[0];
        for (double v : logits) if (v > max) max = v;
        double sum = 0;
        double[] exp = new double[logits.length];
        for (int i = 0; i < logits.length; i++) { exp[i] = Math.exp(logits[i] - max); sum += exp[i]; }
        for (int i = 0; i < exp.length; i++) exp[i] /= sum;
        return exp;
    }

    private int argmax(double[] arr) {
        int idx = 0;
        for (int i = 1; i < arr.length; i++) if (arr[i] > arr[idx]) idx = i;
        return idx;
    }

    private double normalize(double value, double min, double max) {
        return Math.max(0, Math.min(1, (value - min) / (max - min)));
    }

    public record PredictionResult(
        RiskLevel riskLevel,
        double probabilitySafe,
        double probabilityWarning,
        double probabilityCritical,
        double overallRiskScore,
        String modelVersion
    ) {}
}

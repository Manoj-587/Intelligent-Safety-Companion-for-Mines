package com.minecompanion.simulator;

import com.minecompanion.persistence.controller.SensorController;
import com.minecompanion.persistence.dto.SensorReadingRequest;
import com.minecompanion.persistence.entity.PredictionHistory;
import com.minecompanion.persistence.service.PredictionHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Random;

/**
 * Underground Mine Sensor Simulator Service.
 *
 * Continuously generates realistic underground sensor readings every 5 seconds
 * and feeds them directly through the existing SensorController & Service pipeline into:
 *   SensorController -> SensorReadingService -> MySQL -> Flask ML API -> PredictionHistory -> CompanionService
 *
 * Deterministically cycles through 3 scenarios (3 LOW -> 3 MEDIUM -> 3 HIGH -> repeat).
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "simulation.enabled", havingValue = "true", matchIfMissing = true)
public class SensorSimulatorService {

    private final SensorController sensorController;
    private final PredictionHistoryService predictionHistoryService;
    private final Random random = new Random();

    public enum RiskScenario {
        LOW("LOW", 29.0, 0.5, 60.0, 2.0, 0.08, 0.02, 3.0, 0.5, 20.8, 0.2, 3.5, 0.2, 101.3, 0.2),
        MEDIUM("MEDIUM", 32.0, 0.5, 65.0, 2.0, 1.20, 0.10, 20.0, 3.0, 20.2, 0.2, 2.8, 0.2, 101.1, 0.2),
        HIGH("HIGH", 35.0, 0.5, 72.0, 2.0, 2.40, 0.10, 40.0, 3.0, 19.8, 0.2, 1.8, 0.2, 100.9, 0.2);

        private final String label;
        private final double temp;
        private final double tempDelta;
        private final double humidity;
        private final double humidityDelta;
        private final double methane;
        private final double methaneDelta;
        private final double co;
        private final double coDelta;
        private final double oxygen;
        private final double oxygenDelta;
        private final double airflow;
        private final double airflowDelta;
        private final double pressure;
        private final double pressureDelta;

        RiskScenario(String label, double temp, double tempDelta, double humidity, double humidityDelta,
                     double methane, double methaneDelta, double co, double coDelta,
                     double oxygen, double oxygenDelta, double airflow, double airflowDelta,
                     double pressure, double pressureDelta) {
            this.label = label;
            this.temp = temp;
            this.tempDelta = tempDelta;
            this.humidity = humidity;
            this.humidityDelta = humidityDelta;
            this.methane = methane;
            this.methaneDelta = methaneDelta;
            this.co = co;
            this.coDelta = coDelta;
            this.oxygen = oxygen;
            this.oxygenDelta = oxygenDelta;
            this.airflow = airflow;
            this.airflowDelta = airflowDelta;
            this.pressure = pressure;
            this.pressureDelta = pressureDelta;
        }

        public String getLabel() { return label; }
    }

    private RiskScenario currentScenario = RiskScenario.LOW;
    private int scenarioReadingCount = 0;

    @Scheduled(fixedRateString = "${simulation.interval-ms:5000}")
    public void generateSensorReading() {
        scenarioReadingCount++;

        if (scenarioReadingCount > 3) {
            RiskScenario[] scenarios = RiskScenario.values();
            int nextIndex = (currentScenario.ordinal() + 1) % scenarios.length;
            currentScenario = scenarios[nextIndex];
            scenarioReadingCount = 1;
        }

        // Generate sensor values with small random fluctuations within specified ranges
        double temp     = Math.round((currentScenario.temp + noise(currentScenario.tempDelta)) * 10.0) / 10.0;
        double humidity = Math.round((currentScenario.humidity + noise(currentScenario.humidityDelta)) * 10.0) / 10.0;
        double methane  = Math.round((currentScenario.methane + noise(currentScenario.methaneDelta)) * 100.0) / 100.0;
        double co       = Math.round((currentScenario.co + noise(currentScenario.coDelta)) * 10.0) / 10.0;
        double oxygen   = Math.round((currentScenario.oxygen + noise(currentScenario.oxygenDelta)) * 10.0) / 10.0;
        double airflow  = Math.round((currentScenario.airflow + noise(currentScenario.airflowDelta)) * 10.0) / 10.0;
        double pressure = Math.round((currentScenario.pressure + noise(currentScenario.pressureDelta)) * 10.0) / 10.0;

        SensorReadingRequest request = new SensorReadingRequest();
        request.setSessionId("simulated-mine-session");
        request.setTemperature(temp);
        request.setHumidity(humidity);
        request.setMethane(methane);
        request.setCarbonMonoxide(co);
        request.setOxygen(oxygen);
        request.setAirflow(airflow);
        request.setPressure(pressure);

        try {
            sensorController.createSensorReading(request);

            Optional<PredictionHistory> latestPrediction = predictionHistoryService.getLatest();
            String actualPrediction = latestPrediction.map(PredictionHistory::getRiskLevel).orElse("UNKNOWN");

            StringBuilder logBuilder = new StringBuilder();
            logBuilder.append("\n------------------------------------------------\n")
                      .append("[Simulator]\n")
                      .append("Scenario            : ").append(currentScenario.getLabel()).append("\n")
                      .append("Reading             : ").append(scenarioReadingCount).append(" / 3\n")
                      .append("Prediction Expected : ").append(currentScenario.getLabel()).append("\n")
                      .append("Prediction Actual   : ").append(actualPrediction);

            if (scenarioReadingCount == 3) {
                RiskScenario nextScenario = RiskScenario.values()[(currentScenario.ordinal() + 1) % RiskScenario.values().length];
                logBuilder.append("\nSwitching to ").append(nextScenario.getLabel());
            }

            logBuilder.append("\n------------------------------------------------");
            log.info(logBuilder.toString());

        } catch (Exception ex) {
            log.error("[Simulator] Failed to execute simulated sensor reading pipeline: {}", ex.getMessage(), ex);
        }
    }

    private double noise(double delta) {
        return (random.nextDouble() * 2.0 - 1.0) * delta;
    }
}

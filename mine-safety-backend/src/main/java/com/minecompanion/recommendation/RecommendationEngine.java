package com.minecompanion.recommendation;

import com.minecompanion.persistence.entity.SensorReading;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Intelligent Dynamic Recommendation Engine.
 * Generates context-aware, explainable safety recommendations based on
 * ML Prediction Risk Level and Live Sensor Values.
 */
@Slf4j
@Service
public class RecommendationEngine {

    /**
     * Generates a list of intelligent recommendations based on ML Prediction Risk Level and Live Sensor Values.
     *
     * @param riskLevel ML prediction risk ("LOW", "MEDIUM", "HIGH", "CRITICAL")
     * @param reading   latest live sensor reading
     * @return List of at most 6 deduplicated, priority-sorted recommendations.
     */
    public List<Recommendation> generate(String riskLevel, SensorReading reading) {
        String risk = riskLevel != null ? riskLevel.toUpperCase() : "LOW";
        List<Recommendation> rawList = new ArrayList<>();

        double methane = reading != null && reading.getMethane() != null ? reading.getMethane() : 0.0;
        double co = reading != null && reading.getCarbonMonoxide() != null ? reading.getCarbonMonoxide() : 0.0;
        double oxygen = reading != null && reading.getOxygen() != null ? reading.getOxygen() : 20.9;
        double temp = reading != null && reading.getTemperature() != null ? reading.getTemperature() : 25.0;
        double humidity = reading != null && reading.getHumidity() != null ? reading.getHumidity() : 50.0;
        double airflow = reading != null && reading.getAirflow() != null ? reading.getAirflow() : 3.0;

        // ── 1. Sensor-Specific Trigger Rules ──────────────────────────────────────
        if (methane >= 2.0) {
            rawList.add(Recommendation.builder()
                    .title("Stop Ignition Sources Immediately")
                    .description("De-energize non-explosion-proof electrical equipment and cut power to shearers.")
                    .reason("Methane reached " + methane + "% (exceeds 2.0% explosive safety limit).")
                    .priority(Recommendation.Priority.HIGH)
                    .category(Recommendation.Category.IMMEDIATE)
                    .iconName("FireIcon")
                    .color("#d32f2f")
                    .sensorTrigger("Methane Sensor")
                    .build());

            rawList.add(Recommendation.builder()
                    .title("Increase Ventilation Immediately")
                    .description("Maximize main surface fan output and open ventilation doors to flush gas.")
                    .reason("Methane concentration reached " + methane + "%.")
                    .priority(Recommendation.Priority.HIGH)
                    .category(Recommendation.Category.IMMEDIATE)
                    .iconName("VentilationIcon")
                    .color("#d32f2f")
                    .sensorTrigger("Methane Sensor")
                    .build());

            rawList.add(Recommendation.builder()
                    .title("Inspect Methane Extraction System")
                    .description("Verify drainage pipe pressure and suction pumps for gas extraction.")
                    .reason("High methane concentration (" + methane + "%).")
                    .priority(Recommendation.Priority.HIGH)
                    .category(Recommendation.Category.INSPECTION)
                    .iconName("ReportIcon")
                    .color("#d32f2f")
                    .sensorTrigger("Methane Sensor")
                    .build());

            rawList.add(Recommendation.builder()
                    .title("Prepare Evacuation")
                    .description("Halt mining operations and prepare personnel for orderly retreat to fresh air base.")
                    .reason("Methane reached " + methane + "% hazardous level.")
                    .priority(Recommendation.Priority.HIGH)
                    .category(Recommendation.Category.EMERGENCY)
                    .iconName("WarningIcon")
                    .color("#d32f2f")
                    .sensorTrigger("Methane Sensor")
                    .build());
        } else if (methane >= 1.0) {
            rawList.add(Recommendation.builder()
                    .title("Inspect Methane Extraction System")
                    .description("Verify drainage pipe pressure and suction pumps for gas extraction.")
                    .reason("Methane level elevated at " + methane + "%.")
                    .priority(Recommendation.Priority.MEDIUM)
                    .category(Recommendation.Category.INSPECTION)
                    .iconName("ReportIcon")
                    .color("#ed6c02")
                    .sensorTrigger("Methane Sensor")
                    .build());

            rawList.add(Recommendation.builder()
                    .title("Increase Ventilation")
                    .description("Boost auxiliary fans in the active working face.")
                    .reason("Methane level elevated at " + methane + "%.")
                    .priority(Recommendation.Priority.MEDIUM)
                    .category(Recommendation.Category.MONITORING)
                    .iconName("VentilationIcon")
                    .color("#ed6c02")
                    .sensorTrigger("Methane Sensor")
                    .build());
        }

        if (co >= 35.0) {
            rawList.add(Recommendation.builder()
                    .title("Wear Respiratory Protection")
                    .description("Don self-contained self-rescuers (SCSR) immediately.")
                    .reason("Carbon monoxide reached " + co + " ppm (toxic threshold).")
                    .priority(Recommendation.Priority.HIGH)
                    .category(Recommendation.Category.IMMEDIATE)
                    .iconName("WarningIcon")
                    .color("#d32f2f")
                    .sensorTrigger("Carbon Monoxide Sensor")
                    .build());

            rawList.add(Recommendation.builder()
                    .title("Locate Combustion Source")
                    .description("Inspect active seams and conveyor belts for smoldering or fire.")
                    .reason("CO level at " + co + " ppm indicates combustion.")
                    .priority(Recommendation.Priority.HIGH)
                    .category(Recommendation.Category.EMERGENCY)
                    .iconName("FireIcon")
                    .color("#d32f2f")
                    .sensorTrigger("Carbon Monoxide Sensor")
                    .build());
        } else if (co >= 15.0) {
            rawList.add(Recommendation.builder()
                    .title("Inspect Diesel Machinery")
                    .description("Check exhaust scrubbers and engine emissions on underground vehicles.")
                    .reason("Carbon monoxide elevated at " + co + " ppm.")
                    .priority(Recommendation.Priority.MEDIUM)
                    .category(Recommendation.Category.INSPECTION)
                    .iconName("ReportIcon")
                    .color("#ed6c02")
                    .sensorTrigger("Carbon Monoxide Sensor")
                    .build());

            rawList.add(Recommendation.builder()
                    .title("Increase Fresh Airflow")
                    .description("Direct additional fresh air to clear carbon monoxide buildup.")
                    .reason("CO level elevated at " + co + " ppm.")
                    .priority(Recommendation.Priority.MEDIUM)
                    .category(Recommendation.Category.MONITORING)
                    .iconName("VentilationIcon")
                    .color("#ed6c02")
                    .sensorTrigger("Carbon Monoxide Sensor")
                    .build());
        }

        if (oxygen < 19.5) {
            rawList.add(Recommendation.builder()
                    .title("Increase Ventilation")
                    .description("Supply fresh atmospheric air to restore normal oxygen concentration.")
                    .reason("Oxygen dropped to " + oxygen + "% (below 19.5% mandatory threshold).")
                    .priority(Recommendation.Priority.HIGH)
                    .category(Recommendation.Category.IMMEDIATE)
                    .iconName("VentilationIcon")
                    .color("#d32f2f")
                    .sensorTrigger("Oxygen Sensor")
                    .build());

            rawList.add(Recommendation.builder()
                    .title("Prepare Oxygen Equipment")
                    .description("Stage emergency oxygen apparatus and check refuge chambers.")
                    .reason("Depleted oxygen level (" + oxygen + "%).")
                    .priority(Recommendation.Priority.HIGH)
                    .category(Recommendation.Category.EMERGENCY)
                    .iconName("ShieldIcon")
                    .color("#d32f2f")
                    .sensorTrigger("Oxygen Sensor")
                    .build());
        }

        if (temp >= 34.0) {
            rawList.add(Recommendation.builder()
                    .title("Reduce Equipment Load")
                    .description("Throttle heavy motors and mechanical drives to limit heat generation.")
                    .reason("Ambient temperature reached " + temp + "°C.")
                    .priority(Recommendation.Priority.MEDIUM)
                    .category(Recommendation.Category.PREVENTIVE)
                    .iconName("WarningIcon")
                    .color("#ed6c02")
                    .sensorTrigger("Temperature Sensor")
                    .build());

            rawList.add(Recommendation.builder()
                    .title("Inspect Cooling Systems")
                    .description("Check mine chiller plant and water spray systems.")
                    .reason("High underground temperature (" + temp + "°C).")
                    .priority(Recommendation.Priority.MEDIUM)
                    .category(Recommendation.Category.MAINTENANCE)
                    .iconName("ReportIcon")
                    .color("#ed6c02")
                    .sensorTrigger("Temperature Sensor")
                    .build());
        }

        if (airflow < 2.0) {
            rawList.add(Recommendation.builder()
                    .title("Inspect Auxiliary Fans")
                    .description("Check ducting connections, power supplies, and fan impellers.")
                    .reason("Airflow velocity dropped to " + airflow + " m/s.")
                    .priority(Recommendation.Priority.MEDIUM)
                    .category(Recommendation.Category.INSPECTION)
                    .iconName("VentilationIcon")
                    .color("#ed6c02")
                    .sensorTrigger("Airflow Sensor")
                    .build());

            rawList.add(Recommendation.builder()
                    .title("Check Airflow Obstruction")
                    .description("Inspect airways for roof falls or canvas stopping damage.")
                    .reason("Low ventilation velocity (" + airflow + " m/s).")
                    .priority(Recommendation.Priority.MEDIUM)
                    .category(Recommendation.Category.MAINTENANCE)
                    .iconName("ReportIcon")
                    .color("#ed6c02")
                    .sensorTrigger("Airflow Sensor")
                    .build());
        }

        if (humidity >= 70.0) {
            rawList.add(Recommendation.builder()
                    .title("Check Electrical Insulation")
                    .description("Inspect switchgear and motor enclosures for moisture ingress.")
                    .reason("Relative humidity reached " + humidity + "%.")
                    .priority(Recommendation.Priority.LOW)
                    .category(Recommendation.Category.MAINTENANCE)
                    .iconName("ShieldIcon")
                    .color("#0288d1")
                    .sensorTrigger("Humidity Sensor")
                    .build());

            rawList.add(Recommendation.builder()
                    .title("Inspect Drainage")
                    .description("Check sump pumps and water removal channels.")
                    .reason("High humidity (" + humidity + "%).")
                    .priority(Recommendation.Priority.LOW)
                    .category(Recommendation.Category.INSPECTION)
                    .iconName("ReportIcon")
                    .color("#0288d1")
                    .sensorTrigger("Humidity Sensor")
                    .build());
        }

        // ── 2. Risk Level Baseline Rules ──────────────────────────────────────────
        switch (risk) {
            case "LOW":
            case "SAFE":
                rawList.add(Recommendation.builder()
                        .title("Continue Normal Operations")
                        .description("Maintain standard production workflow with routine safety awareness.")
                        .reason("All sensor readings are operating within safe parameters.")
                        .priority(Recommendation.Priority.LOW)
                        .category(Recommendation.Category.PREVENTIVE)
                        .iconName("CheckIcon")
                        .color("#2e7d32")
                        .sensorTrigger("Routine Schedule")
                        .build());

                rawList.add(Recommendation.builder()
                        .title("Monitor Gas Levels Every 30 Minutes")
                        .description("Conduct regular multi-gas monitor checks across active mine working faces.")
                        .reason("Routine environmental safety compliance check.")
                        .priority(Recommendation.Priority.LOW)
                        .category(Recommendation.Category.MONITORING)
                        .iconName("ReportIcon")
                        .color("#0288d1")
                        .sensorTrigger("Routine Schedule")
                        .build());

                rawList.add(Recommendation.builder()
                        .title("Schedule Routine Ventilation Inspection")
                        .description("Verify main fan differential pressure and stoppings during shift change.")
                        .reason("Periodic ventilation maintenance schedule.")
                        .priority(Recommendation.Priority.LOW)
                        .category(Recommendation.Category.INSPECTION)
                        .iconName("VentilationIcon")
                        .color("#0288d1")
                        .sensorTrigger("Routine Schedule")
                        .build());

                rawList.add(Recommendation.builder()
                        .title("Perform Routine Equipment Inspection")
                        .description("Inspect electrical enclosures and conveyor bearings for normal operating temperature.")
                        .reason("Standard preventive maintenance.")
                        .priority(Recommendation.Priority.LOW)
                        .category(Recommendation.Category.MAINTENANCE)
                        .iconName("ShieldIcon")
                        .color("#2e7d32")
                        .sensorTrigger("Routine Schedule")
                        .build());
                break;

            case "MEDIUM":
            case "WARNING":
                rawList.add(Recommendation.builder()
                        .title("Monitor Gases Every 10 Minutes")
                        .description("Perform frequent portable gas readings at roof line and return airways.")
                        .reason("Precautionary monitoring under elevated risk conditions.")
                        .priority(Recommendation.Priority.MEDIUM)
                        .category(Recommendation.Category.MONITORING)
                        .iconName("ReportIcon")
                        .color("#0288d1")
                        .sensorTrigger("Methane Sensor")
                        .build());

                rawList.add(Recommendation.builder()
                        .title("Inform Supervisor if Methane Continues Rising")
                        .description("Report rising gas concentrations to control room for supervisory oversight.")
                        .reason("Precautionary safety escalation.")
                        .priority(Recommendation.Priority.MEDIUM)
                        .category(Recommendation.Category.INSPECTION)
                        .iconName("ReportIcon")
                        .color("#ed6c02")
                        .sensorTrigger("Routine Schedule")
                        .build());
                break;

            case "HIGH":
            case "CRITICAL":
                rawList.add(Recommendation.builder()
                        .title("Continuous Gas Monitoring")
                        .description("Deploy telemetry sensors for continuous real-time gas tracking.")
                        .reason("Active hazard tracking under high risk.")
                        .priority(Recommendation.Priority.HIGH)
                        .category(Recommendation.Category.MONITORING)
                        .iconName("ReportIcon")
                        .color("#d32f2f")
                        .sensorTrigger("Methane Sensor")
                        .build());

                rawList.add(Recommendation.builder()
                        .title("Notify Control Room")
                        .description("Contact surface control room immediately to report high-risk environmental status.")
                        .reason("High-risk status alert.")
                        .priority(Recommendation.Priority.HIGH)
                        .category(Recommendation.Category.IMMEDIATE)
                        .iconName("WarningIcon")
                        .color("#d32f2f")
                        .sensorTrigger("Routine Schedule")
                        .build());
                break;
        }

        // ── 3. Strict Rule Enforcements ──────────────────────────────────────────
        if ("LOW".equals(risk) || "SAFE".equals(risk)) {
            rawList.removeIf(r -> r.getCategory() == Recommendation.Category.EMERGENCY ||
                                  r.getPriority() == Recommendation.Priority.HIGH ||
                                  r.getTitle().toLowerCase().contains("evacuate") ||
                                  r.getTitle().toLowerCase().contains("danger"));
        }

        // ── 4. Merge Duplicate Recommendations ───────────────────────────────────
        Map<String, Recommendation> mergedMap = new LinkedHashMap<>();
        for (Recommendation r : rawList) {
            String title = r.getTitle();
            if (mergedMap.containsKey(title)) {
                Recommendation existing = mergedMap.get(title);
                if (!existing.getReason().contains(r.getReason())) {
                    existing.setReason(existing.getReason() + "; " + r.getReason());
                }
                if (r.getPriority() == Recommendation.Priority.HIGH) {
                    existing.setPriority(Recommendation.Priority.HIGH);
                }
            } else {
                mergedMap.put(title, r);
            }
        }

        // ── 5. Sort by Priority (HIGH -> MEDIUM -> LOW) & Limit to 6 ─────────────
        return mergedMap.values().stream()
                .sorted((a, b) -> Integer.compare(priorityRank(a.getPriority()), priorityRank(b.getPriority())))
                .limit(6)
                .collect(Collectors.toList());
    }

    private int priorityRank(Recommendation.Priority priority) {
        if (priority == Recommendation.Priority.HIGH) return 1;
        if (priority == Recommendation.Priority.MEDIUM) return 2;
        return 3;
    }
}

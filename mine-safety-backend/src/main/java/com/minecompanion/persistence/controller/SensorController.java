package com.minecompanion.persistence.controller;

import com.minecompanion.aiml.AimlApiClient;
import com.minecompanion.exception.AimlUnavailableException;
import com.minecompanion.aiml.PredictionResult;
import com.minecompanion.persistence.dto.SensorReadingRequest;
import com.minecompanion.persistence.dto.SensorReadingResponse;
import com.minecompanion.persistence.entity.PredictionHistory;
import com.minecompanion.persistence.entity.SensorReading;
import com.minecompanion.persistence.service.PredictionHistoryService;
import com.minecompanion.persistence.service.SensorReadingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sensors")
@RequiredArgsConstructor
@Slf4j
public class SensorController {

    private final SensorReadingService sensorReadingService;
    private final PredictionHistoryService predictionHistoryService;
    private final AimlApiClient aimlApiClient;
    private final com.minecompanion.recommendation.RecommendationEngine recommendationEngine;

    @PostMapping
    public ResponseEntity<SensorReadingResponse> createSensorReading(@RequestBody SensorReadingRequest request) {
        SensorReading reading = SensorReading.builder()
                .sessionId(request.getSessionId() != null ? request.getSessionId() : "default-session")
                .temperature(request.getTemperature())
                .humidity(request.getHumidity())
                .methane(request.getMethane())
                .carbonMonoxide(request.getCarbonMonoxide())
                .oxygen(request.getOxygen())
                .airflow(request.getAirflow())
                .pressure(request.getPressure())
                .timestamp(LocalDateTime.now())
                .build();

        SensorReading savedReading = sensorReadingService.save(reading);

        // Trigger Flask prediction and persist in PredictionHistory
        String riskLevel = "MEDIUM";

        try {
            Map<String, Object> payload = buildFlaskPayload(savedReading);
            PredictionResult result = aimlApiClient.predict(payload);
            riskLevel = result.getPredictedRisk();
        } catch (Exception ex) {
            log.warn("[SensorController] Flask prediction unavailable — using rule-based fallback for persistence: {}", ex.getMessage());
            if (savedReading.getMethane() != null && savedReading.getMethane() >= 2.0) {
                riskLevel = "HIGH";
            } else if (savedReading.getMethane() != null && savedReading.getMethane() >= 1.0) {
                riskLevel = "MEDIUM";
            } else {
                riskLevel = "LOW";
            }
        }

        // Generate dynamic explainable recommendations from RecommendationEngine
        List<com.minecompanion.recommendation.Recommendation> recList = recommendationEngine.generate(riskLevel, savedReading);
        String recommendation = recList.stream()
                .map(r -> "[" + r.getCategory().name() + "] " + r.getTitle() + ": " + r.getDescription() + " (Reason: " + r.getReason() + "; Triggered by: " + r.getSensorTrigger() + "; Priority: " + r.getPriority().name() + ")")
                .collect(java.util.stream.Collectors.joining("; "));

        PredictionHistory history = PredictionHistory.builder()
                .sessionId(savedReading.getSessionId())
                .riskLevel(riskLevel)
                .confidence(0.97)
                .recommendation(recommendation)
                .createdAt(LocalDateTime.now())
                .sensorReading(savedReading)
                .build();

        predictionHistoryService.save(history);
        log.info("[SensorController] Saved prediction history | risk={} sessionId={}",
                riskLevel, savedReading.getSessionId());

        return ResponseEntity.ok(SensorReadingResponse.fromEntity(savedReading));
    }

    @GetMapping("/latest")
    public ResponseEntity<SensorReadingResponse> getLatestSensorReading() {
        return sensorReadingService.getLatest()
                .map(reading -> ResponseEntity.ok(SensorReadingResponse.fromEntity(reading)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/history")
    public ResponseEntity<List<SensorReadingResponse>> getSensorHistory() {
        List<SensorReadingResponse> list = sensorReadingService.getRecent()
                .stream()
                .map(SensorReadingResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(list);
    }

    private Map<String, Object> buildFlaskPayload(SensorReading r) {
        Map<String, Object> map = new HashMap<>();
        // Environmental & gas readings
        map.put("AN311", r.getCarbonMonoxide() != null ? r.getCarbonMonoxide() : 10.0);
        map.put("AN422", r.getMethane() != null ? r.getMethane() : 0.5);
        map.put("AN423", 0.1);
        map.put("TP1721", r.getTemperature() != null ? r.getTemperature() : 25.0);
        map.put("RH1722", r.getHumidity() != null ? r.getHumidity() : 50.0);
        map.put("BA1723", r.getPressure() != null ? r.getPressure() : 101.3);
        map.put("TP1711", 24.5);
        map.put("RH1712", 48.0);
        map.put("BA1713", r.getAirflow() != null ? r.getAirflow() : 2.5);
        // Mine monitoring channels
        map.put("MM252", 0.0);
        map.put("MM261", 0.0);
        map.put("MM262", 0.0);
        map.put("MM263", 0.0);
        map.put("MM264", r.getOxygen() != null ? r.getOxygen() : 20.9);
        map.put("MM256", 0.0);
        map.put("MM211", 0.0);
        map.put("CM861", 0.0);
        map.put("CR863", 0.0);
        map.put("P_864", 0.0);
        map.put("TC862", 0.0);
        map.put("WM868", 0.0);
        // Infrared & current sensors
        map.put("AMP1_IR", 12.0);
        map.put("AMP2_IR", 12.5);
        map.put("DMP3_IR", 5.0);
        map.put("DMP4_IR", 5.2);
        map.put("AMP5_IR", 11.8);
        map.put("F_SIDE", 1.0);
        return map;
    }
}

package com.minecompanion.persistence.dto;

import com.minecompanion.persistence.entity.PredictionHistory;
import com.minecompanion.recommendation.Recommendation;
import com.minecompanion.recommendation.RecommendationEngine;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PredictionHistoryResponse {
    private Long id;
    private String sessionId;
    private String riskLevel;
    private Double confidence;
    private String recommendation;
    private List<RecommendationResponse> detailedRecommendations;
    private LocalDateTime createdAt;
    private SensorReadingResponse sensorReading;

    public static PredictionHistoryResponse fromEntity(PredictionHistory entity) {
        if (entity == null) return null;

        List<RecommendationResponse> detailed = null;
        if (entity.getRiskLevel() != null) {
            RecommendationEngine engine = new RecommendationEngine();
            List<Recommendation> recs = engine.generate(entity.getRiskLevel(), entity.getSensorReading());
            detailed = recs.stream().map(RecommendationResponse::fromEntity).toList();
        }

        return PredictionHistoryResponse.builder()
                .id(entity.getId())
                .sessionId(entity.getSessionId())
                .riskLevel(entity.getRiskLevel())
                .confidence(entity.getConfidence())
                .recommendation(entity.getRecommendation())
                .detailedRecommendations(detailed)
                .createdAt(entity.getCreatedAt())
                .sensorReading(SensorReadingResponse.fromEntity(entity.getSensorReading()))
                .build();
    }
}

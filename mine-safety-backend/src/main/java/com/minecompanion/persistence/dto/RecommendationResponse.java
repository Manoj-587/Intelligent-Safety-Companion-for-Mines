package com.minecompanion.persistence.dto;

import com.minecompanion.recommendation.Recommendation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecommendationResponse {
    private String title;
    private String description;
    private String reason;
    private String priority;
    private String category;
    private String iconName;
    private String color;
    private String sensorTrigger;

    public static RecommendationResponse fromEntity(Recommendation r) {
        if (r == null) return null;
        return RecommendationResponse.builder()
                .title(r.getTitle())
                .description(r.getDescription())
                .reason(r.getReason())
                .priority(r.getPriority() != null ? r.getPriority().name() : "LOW")
                .category(r.getCategory() != null ? r.getCategory().name() : "PREVENTIVE")
                .iconName(r.getIconName())
                .color(r.getColor())
                .sensorTrigger(r.getSensorTrigger())
                .build();
    }
}

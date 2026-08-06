package com.minecompanion.recommendation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Recommendation {

    public enum Priority {
        HIGH, MEDIUM, LOW
    }

    public enum Category {
        IMMEDIATE, MONITORING, INSPECTION, PREVENTIVE, MAINTENANCE, EMERGENCY
    }

    private String title;
    private String description;
    private String reason;
    private Priority priority;
    private Category category;
    private String iconName;
    private String color;
    private String sensorTrigger;
}

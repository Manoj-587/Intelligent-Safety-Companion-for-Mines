package com.minecompanion.persistence.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SensorReadingRequest {
    private String sessionId;
    private Double temperature;
    private Double humidity;
    private Double methane;
    private Double carbonMonoxide;
    private Double oxygen;
    private Double airflow;
    private Double pressure;
}

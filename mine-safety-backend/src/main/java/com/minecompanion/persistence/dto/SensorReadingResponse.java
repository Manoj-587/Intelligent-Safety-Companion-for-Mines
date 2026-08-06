package com.minecompanion.persistence.dto;

import com.minecompanion.persistence.entity.SensorReading;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SensorReadingResponse {
    private Long id;
    private String sessionId;
    private Double temperature;
    private Double humidity;
    private Double methane;
    private Double carbonMonoxide;
    private Double oxygen;
    private Double airflow;
    private Double pressure;
    private LocalDateTime timestamp;

    public static SensorReadingResponse fromEntity(SensorReading entity) {
        if (entity == null) return null;
        return SensorReadingResponse.builder()
                .id(entity.getId())
                .sessionId(entity.getSessionId())
                .temperature(entity.getTemperature())
                .humidity(entity.getHumidity())
                .methane(entity.getMethane())
                .carbonMonoxide(entity.getCarbonMonoxide())
                .oxygen(entity.getOxygen())
                .airflow(entity.getAirflow())
                .pressure(entity.getPressure())
                .timestamp(entity.getTimestamp())
                .build();
    }
}

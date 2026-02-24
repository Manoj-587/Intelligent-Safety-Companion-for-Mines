package com.minesafety.entity;

import com.minesafety.enums.RiskLevel;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "risk_predictions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RiskPrediction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sensor_data_id", nullable = false)
    private SensorData sensorData;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mine_id", nullable = false)
    private Mine mine;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "zone_id", nullable = false)
    private Zone zone;

    @Enumerated(EnumType.STRING)
    @Column(name = "predicted_risk", nullable = false)
    private RiskLevel predictedRisk;

    @Column(name = "probability_safe")
    private Double probabilitySafe;

    @Column(name = "probability_warning")
    private Double probabilityWarning;

    @Column(name = "probability_critical")
    private Double probabilityCritical;

    @Column(name = "overall_risk_score", nullable = false)
    private Double overallRiskScore;

    @Column(name = "model_version", length = 50)
    private String modelVersion;

    @CreationTimestamp
    @Column(name = "prediction_time", nullable = false, updatable = false)
    private LocalDateTime predictionTime;
}

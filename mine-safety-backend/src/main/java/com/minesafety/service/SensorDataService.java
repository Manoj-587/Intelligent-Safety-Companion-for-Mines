package com.minesafety.service;

import com.minesafety.entity.Alert;
import com.minesafety.entity.Mine;
import com.minesafety.entity.RiskPrediction;
import com.minesafety.entity.SensorData;
import com.minesafety.enums.AlertStatus;
import com.minesafety.enums.AlertType;
import com.minesafety.enums.RiskLevel;
import com.minesafety.repository.AlertRepo;
import com.minesafety.repository.MineRepo;
import com.minesafety.repository.RiskPredictionRepo;
import com.minesafety.repository.SensorDataRepo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SensorDataService {

    private final SensorDataRepo repository;
    private final RiskPredictionRepo riskPredictionRepo;
    private final AlertRepo alertRepo;
    private final MLPredictionService mlService;
    private final PatternAnalysisService patternService;
    private final MineRepo mineRepo;

    public SensorDataService(SensorDataRepo repository,
                             RiskPredictionRepo riskPredictionRepo,
                             AlertRepo alertRepo,
                             MLPredictionService mlService,
                             PatternAnalysisService patternService,
                             MineRepo mineRepo) {
        this.repository = repository;
        this.riskPredictionRepo = riskPredictionRepo;
        this.alertRepo = alertRepo;
        this.mlService = mlService;
        this.patternService = patternService;
        this.mineRepo = mineRepo;
    }

    @Transactional
    public SensorData create(SensorData sensorData) {
        // 1. ML Prediction
        MLPredictionService.PredictionResult prediction = mlService.predict(sensorData);

        // 2. Pattern Analysis — may escalate the risk level
        RiskLevel finalLevel = patternService.analyzeAndEscalate(sensorData.getZone(), prediction.riskLevel());

        // 3. Enrich sensor data with computed values
        sensorData.setRiskLevel(finalLevel);
        sensorData.setRiskScore(prediction.overallRiskScore());

        SensorData saved = repository.save(sensorData);

        // 4. Save Risk Prediction record
        RiskPrediction rp = new RiskPrediction();
        rp.setSensorData(saved);
        rp.setMine(saved.getMine());
        rp.setZone(saved.getZone());
        rp.setPredictedRisk(finalLevel);
        rp.setProbabilitySafe(round(prediction.probabilitySafe()));
        rp.setProbabilityWarning(round(prediction.probabilityWarning()));
        rp.setProbabilityCritical(round(prediction.probabilityCritical()));
        rp.setOverallRiskScore(prediction.overallRiskScore());
        rp.setModelVersion(prediction.modelVersion());
        riskPredictionRepo.save(rp);

        // 5. Auto-generate Alert if WARNING or CRITICAL (skip if active alert already exists for this zone)
        if (finalLevel != RiskLevel.SAFE) {
            boolean alreadyActive = !alertRepo.findByZoneAndStatus(saved.getZone(), AlertStatus.ACTIVE).isEmpty();
            if (!alreadyActive) {
                Alert alert = new Alert();
                alert.setSensorData(saved);
                alert.setMine(saved.getMine());
                alert.setZone(saved.getZone());
                alert.setRiskLevel(finalLevel);
                alert.setAlertType(determineAlertType(saved));
                alert.setMessage(buildAlertMessage(saved, finalLevel));
                alert.setStatus(AlertStatus.ACTIVE);
                alertRepo.save(alert);
            }
        }

        return saved;
    }

    public List<SensorData> getAll() {
        return repository.findAll();
    }

    public List<SensorData> getByMine(Long mineId) {
        Mine mine = mineRepo.findById(mineId)
                .orElseThrow(() -> new RuntimeException("Mine not found"));
        return repository.findByMineOrderByRecordedAtDesc(mine);
    }

    public SensorData getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("SensorData not found"));
    }

    @Transactional
    public SensorData update(Long id, SensorData sensorData) {
        SensorData existing = getById(id);
        sensorData.setId(existing.getId());
        // Re-run ML on update
        MLPredictionService.PredictionResult prediction = mlService.predict(sensorData);
        sensorData.setRiskLevel(prediction.riskLevel());
        sensorData.setRiskScore(prediction.overallRiskScore());
        return repository.save(sensorData);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }

    private AlertType determineAlertType(SensorData d) {
        boolean gasDanger  = d.getMethaneLevel() > 1.0 || (d.getCarbonMonoxideLevel() != null && d.getCarbonMonoxideLevel() > 25);
        boolean o2Danger   = d.getOxygenLevel() < 19.5;
        boolean tempDanger = d.getTemperature() > 35;
        int count = (gasDanger ? 1 : 0) + (o2Danger ? 1 : 0) + (tempDanger ? 1 : 0);
        if (count > 1) return AlertType.MULTI_FACTOR;
        if (o2Danger)  return AlertType.OXYGEN;
        if (tempDanger) return AlertType.TEMPERATURE;
        return AlertType.GAS;
    }

    private String buildAlertMessage(SensorData d, RiskLevel level) {
        return String.format(
            "%s condition detected in zone %s: CH₄=%.2f%%, O₂=%.2f%%, Temp=%.1f°C, CO=%.1fppm, Risk Score=%.1f",
            level.name(),
            d.getZone().getZoneName(),
            d.getMethaneLevel(),
            d.getOxygenLevel(),
            d.getTemperature(),
            d.getCarbonMonoxideLevel() != null ? d.getCarbonMonoxideLevel() : 0.0,
            d.getRiskScore()
        );
    }

    private double round(double v) {
        return Math.round(v * 10000.0) / 10000.0;
    }
}

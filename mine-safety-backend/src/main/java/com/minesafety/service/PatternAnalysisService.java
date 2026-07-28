package com.minesafety.service;

import com.minesafety.entity.RiskPrediction;
import com.minesafety.entity.Zone;
import com.minesafety.enums.RiskLevel;
import com.minesafety.repository.RiskPredictionRepo;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Analyzes recent risk predictions for a zone to detect escalating trends.
 * If the last N predictions show a consistently increasing risk score,
 * the risk level is escalated (e.g. WARNING → CRITICAL).
 */
@Service
public class PatternAnalysisService {

    private static final int WINDOW_MINUTES = 30;
    private static final int MIN_SAMPLES = 3;
    // If risk score increases by this much across the window, escalate
    private static final double ESCALATION_THRESHOLD = 20.0;

    private final RiskPredictionRepo riskPredictionRepo;

    public PatternAnalysisService(RiskPredictionRepo riskPredictionRepo) {
        this.riskPredictionRepo = riskPredictionRepo;
    }

    /**
     * Returns the escalated risk level for a zone based on trend analysis.
     * If no escalation is detected, returns the current level unchanged.
     */
    public RiskLevel analyzeAndEscalate(Zone zone, RiskLevel currentLevel) {
        if (currentLevel == RiskLevel.CRITICAL) return RiskLevel.CRITICAL;

        List<RiskPrediction> recent = riskPredictionRepo.findRecentByZone(
            zone, LocalDateTime.now().minusMinutes(WINDOW_MINUTES)
        );

        if (recent.size() < MIN_SAMPLES) return currentLevel;

        double firstScore = recent.get(0).getOverallRiskScore();
        double lastScore  = recent.get(recent.size() - 1).getOverallRiskScore();
        double trend      = lastScore - firstScore;

        // Count how many consecutive recent predictions are non-SAFE
        long unsafeCount = recent.stream()
            .filter(r -> r.getPredictedRisk() != RiskLevel.SAFE)
            .count();

        boolean escalating = trend >= ESCALATION_THRESHOLD;
        boolean repeatedUnsafe = unsafeCount >= MIN_SAMPLES;

        if ((escalating || repeatedUnsafe) && currentLevel == RiskLevel.WARNING) {
            return RiskLevel.CRITICAL;
        }
        if (escalating && currentLevel == RiskLevel.SAFE) {
            return RiskLevel.WARNING;
        }
        return currentLevel;
    }
}

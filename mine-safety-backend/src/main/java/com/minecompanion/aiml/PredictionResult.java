package com.minecompanion.aiml;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Immutable DTO representing the response from the Flask AIML /predict endpoint.
 *
 * Field names match the Flask JSON keys exactly so Jackson deserializes
 * without any custom mapping:
 *   {
 *     "predicted_risk":  "HIGH",
 *     "reasons":         ["...", "..."],
 *     "recommendations": ["...", "..."]
 *   }
 *
 * This class is never modified — it is a read-only mirror of the AIML output.
 * CompanionService, SafetyGuard, and PromptBuilder consume it as-is.
 */
@Data
@Builder
public class PredictionResult {

    private String       predictedRisk;
    private List<String> reasons;
    private List<String> recommendations;
}

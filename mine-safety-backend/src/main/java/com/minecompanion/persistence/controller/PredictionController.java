package com.minecompanion.persistence.controller;

import com.minecompanion.persistence.dto.PredictionHistoryResponse;
import com.minecompanion.persistence.service.PredictionHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/predictions")
@RequiredArgsConstructor
public class PredictionController {

    private final PredictionHistoryService predictionHistoryService;

    @GetMapping("/latest")
    public ResponseEntity<PredictionHistoryResponse> getLatestPrediction() {
        return predictionHistoryService.getLatest()
                .map(prediction -> ResponseEntity.ok(PredictionHistoryResponse.fromEntity(prediction)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}

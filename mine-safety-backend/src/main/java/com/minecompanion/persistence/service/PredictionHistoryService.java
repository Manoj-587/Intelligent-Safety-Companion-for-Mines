package com.minecompanion.persistence.service;

import com.minecompanion.persistence.entity.PredictionHistory;
import com.minecompanion.persistence.repository.PredictionHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PredictionHistoryService {

    private final PredictionHistoryRepository predictionHistoryRepository;

    @Transactional
    public PredictionHistory save(PredictionHistory prediction) {
        return predictionHistoryRepository.save(prediction);
    }

    @Transactional(readOnly = true)
    public Optional<PredictionHistory> getLatest() {
        return predictionHistoryRepository.findTopByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public Optional<PredictionHistory> getLatestBySession(String sessionId) {
        return predictionHistoryRepository.findTopBySessionIdOrderByCreatedAtDesc(sessionId);
    }

    @Transactional(readOnly = true)
    public List<PredictionHistory> getRecent() {
        return predictionHistoryRepository.findTop10ByOrderByCreatedAtDesc();
    }
}

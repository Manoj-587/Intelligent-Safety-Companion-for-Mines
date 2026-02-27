package com.minesafety.service;

import com.minesafety.entity.RiskPrediction;
import com.minesafety.repository.RiskPredictionRepo;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RiskPredictionService {

    private final RiskPredictionRepo repository;

    public RiskPredictionService(RiskPredictionRepo repository) {
        this.repository = repository;
    }

    public RiskPrediction create(RiskPrediction riskPrediction) {
        return repository.save(riskPrediction);
    }

    public List<RiskPrediction> getAll() {
        return repository.findAll();
    }

    public RiskPrediction getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("RiskPrediction not found"));
    }

    public RiskPrediction update(Long id, RiskPrediction riskPrediction) {
        RiskPrediction existing = getById(id);
        riskPrediction.setId(existing.getId());
        return repository.save(riskPrediction);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }
}

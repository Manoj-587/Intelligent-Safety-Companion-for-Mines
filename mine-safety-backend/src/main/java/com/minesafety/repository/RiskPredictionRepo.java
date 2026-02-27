package com.minesafety.repository;

import com.minesafety.entity.RiskPrediction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RiskPredictionRepo extends JpaRepository<RiskPrediction, Long> {
}

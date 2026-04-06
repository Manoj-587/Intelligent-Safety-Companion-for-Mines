package com.minesafety.repository;

import com.minesafety.entity.RiskPrediction;
import com.minesafety.entity.Zone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface RiskPredictionRepo extends JpaRepository<RiskPrediction, Long> {

    @Query("SELECT r FROM RiskPrediction r WHERE r.zone = :zone AND r.predictionTime >= :since ORDER BY r.predictionTime ASC")
    List<RiskPrediction> findRecentByZone(@Param("zone") Zone zone, @Param("since") LocalDateTime since);

    List<RiskPrediction> findByMineId(Long mineId);
}

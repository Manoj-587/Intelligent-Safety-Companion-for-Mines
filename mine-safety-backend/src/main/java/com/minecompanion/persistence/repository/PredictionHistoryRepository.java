package com.minecompanion.persistence.repository;

import com.minecompanion.persistence.entity.PredictionHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PredictionHistoryRepository extends JpaRepository<PredictionHistory, Long> {

    Optional<PredictionHistory> findTopBySessionIdOrderByCreatedAtDesc(String sessionId);

    Optional<PredictionHistory> findTopByOrderByCreatedAtDesc();

    List<PredictionHistory> findTop10ByOrderByCreatedAtDesc();
}

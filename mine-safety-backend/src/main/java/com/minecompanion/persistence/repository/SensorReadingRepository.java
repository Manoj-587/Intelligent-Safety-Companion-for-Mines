package com.minecompanion.persistence.repository;

import com.minecompanion.persistence.entity.SensorReading;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SensorReadingRepository extends JpaRepository<SensorReading, Long> {

    Optional<SensorReading> findTopBySessionIdOrderByTimestampDesc(String sessionId);

    Optional<SensorReading> findTopByOrderByTimestampDesc();

    List<SensorReading> findTop10ByOrderByTimestampDesc();
}

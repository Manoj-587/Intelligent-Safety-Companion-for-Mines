package com.minesafety.repository;

import com.minesafety.entity.Mine;
import com.minesafety.entity.SensorData;
import com.minesafety.entity.Zone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface SensorDataRepo extends JpaRepository<SensorData, Long> {

    List<SensorData> findByZoneOrderByRecordedAtDesc(Zone zone);

    List<SensorData> findByMineOrderByRecordedAtDesc(Mine mine);

    @Query("SELECT s FROM SensorData s WHERE s.zone = :zone AND s.recordedAt >= :since ORDER BY s.recordedAt ASC")
    List<SensorData> findRecentByZone(@Param("zone") Zone zone, @Param("since") LocalDateTime since);

    @Query("SELECT s FROM SensorData s ORDER BY s.recordedAt DESC")
    List<SensorData> findLatest(org.springframework.data.domain.Pageable pageable);
}

package com.minesafety.repository;

import com.minesafety.entity.Alert;
import com.minesafety.entity.Zone;
import com.minesafety.enums.AlertStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AlertRepo extends JpaRepository<Alert, Long> {
    List<Alert> findByZoneAndStatus(Zone zone, AlertStatus status);
    List<Alert> findByStatus(AlertStatus status);
    List<Alert> findByMineId(Long mineId);
}

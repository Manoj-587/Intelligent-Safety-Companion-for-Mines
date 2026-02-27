package com.minesafety.repository;

import com.minesafety.entity.SensorData;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SensorDataRepo extends JpaRepository<SensorData, Long> {
}

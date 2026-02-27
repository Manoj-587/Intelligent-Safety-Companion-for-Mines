package com.minesafety.repository;

import com.minesafety.entity.Alert;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlertRepo extends JpaRepository<Alert, Long> {
}

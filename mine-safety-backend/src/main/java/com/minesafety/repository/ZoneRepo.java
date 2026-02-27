package com.minesafety.repository;

import com.minesafety.entity.Zone;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ZoneRepo extends JpaRepository<Zone, Long> {
}

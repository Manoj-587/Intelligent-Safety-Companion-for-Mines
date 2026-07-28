package com.minesafety.repository;

import com.minesafety.entity.Mine;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MineRepo extends JpaRepository<Mine, Long> {
}

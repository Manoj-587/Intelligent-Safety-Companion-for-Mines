package com.minecompanion.persistence.service;

import com.minecompanion.persistence.entity.SensorReading;
import com.minecompanion.persistence.repository.SensorReadingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SensorReadingService {

    private final SensorReadingRepository sensorReadingRepository;

    @Transactional
    public SensorReading save(SensorReading reading) {
        return sensorReadingRepository.save(reading);
    }

    @Transactional(readOnly = true)
    public Optional<SensorReading> getLatest() {
        return sensorReadingRepository.findTopByOrderByTimestampDesc();
    }

    @Transactional(readOnly = true)
    public Optional<SensorReading> getLatestBySession(String sessionId) {
        return sensorReadingRepository.findTopBySessionIdOrderByTimestampDesc(sessionId);
    }

    @Transactional(readOnly = true)
    public List<SensorReading> getRecent() {
        return sensorReadingRepository.findTop10ByOrderByTimestampDesc();
    }
}

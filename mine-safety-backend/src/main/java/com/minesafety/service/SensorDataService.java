package com.minesafety.service;

import com.minesafety.entity.SensorData;
import com.minesafety.repository.SensorDataRepo;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SensorDataService {

    private final SensorDataRepo repository;

    public SensorDataService(SensorDataRepo repository) {
        this.repository = repository;
    }

    public SensorData create(SensorData sensorData) {
        return repository.save(sensorData);
    }

    public List<SensorData> getAll() {
        return repository.findAll();
    }

    public SensorData getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("SensorData not found"));
    }

    public SensorData update(Long id, SensorData sensorData) {
        SensorData existing = getById(id);
        sensorData.setId(existing.getId());
        return repository.save(sensorData);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }
}

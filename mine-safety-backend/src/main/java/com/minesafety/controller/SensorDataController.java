package com.minesafety.controller;

import com.minesafety.entity.SensorData;
import com.minesafety.service.SensorDataService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sensor-data")
@CrossOrigin
public class SensorDataController {

    private final SensorDataService service;

    public SensorDataController(SensorDataService service) {
        this.service = service;
    }

    @PostMapping
    public SensorData create(@RequestBody SensorData sensorData) {
        return service.create(sensorData);
    }

    @GetMapping
    public List<SensorData> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public SensorData getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @PutMapping("/{id}")
    public SensorData update(@PathVariable Long id, @RequestBody SensorData sensorData) {
        return service.update(id, sensorData);
    }

    @DeleteMapping("/{id}")
    public String delete(@PathVariable Long id) {
        service.delete(id);
        return "SensorData deleted successfully";
    }
}

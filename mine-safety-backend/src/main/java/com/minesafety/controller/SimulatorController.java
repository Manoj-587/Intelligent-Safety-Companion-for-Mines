package com.minesafety.controller;

import com.minesafety.service.DataSimulatorService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/simulator")
@CrossOrigin
public class SimulatorController {

    private final DataSimulatorService simulatorService;

    public SimulatorController(DataSimulatorService simulatorService) {
        this.simulatorService = simulatorService;
    }

    @PostMapping("/trigger")
    public Map<String, String> trigger() {
        simulatorService.generateSensorData();
        return Map.of("status", "triggered", "message", "Sensor data generation cycle executed");
    }
}

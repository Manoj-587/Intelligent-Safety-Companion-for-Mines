package com.minesafety.service;

import com.minesafety.entity.Mine;
import com.minesafety.entity.SensorData;
import com.minesafety.entity.Zone;
import com.minesafety.enums.RiskLevel;
import com.minesafety.enums.Status;
import com.minesafety.repository.MineRepo;
import com.minesafety.repository.ZoneRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Random;

/**
 * Simulates continuous mine sensor data generation.
 * Runs every 10 seconds for all active zones.
 * Occasionally injects spikes to simulate dangerous conditions.
 */
@Service
public class DataSimulatorService {

    private static final Logger log = LoggerFactory.getLogger(DataSimulatorService.class);

    private final ZoneRepo zoneRepo;
    private final MineRepo mineRepo;
    private final SensorDataService sensorDataService;
    private final Random random = new Random();

    // Spike counter per zone (simulates gradual deterioration)
    private int cycleCount = 0;

    public DataSimulatorService(ZoneRepo zoneRepo, MineRepo mineRepo, SensorDataService sensorDataService) {
        this.zoneRepo = zoneRepo;
        this.mineRepo = mineRepo;
        this.sensorDataService = sensorDataService;
    }

    @Scheduled(fixedDelay = 10000) // every 10 seconds
    public void generateSensorData() {
        List<Zone> zones = zoneRepo.findAll();
        if (zones.isEmpty()) return;

        cycleCount++;
        // Every 5th cycle, inject a spike in one random zone to simulate danger
        boolean injectSpike = (cycleCount % 5 == 0);
        int spikeZoneIdx = random.nextInt(zones.size());

        for (int i = 0; i < zones.size(); i++) {
            Zone zone = zones.get(i);
            if (zone.getMine() == null) continue;

            boolean spike = injectSpike && (i == spikeZoneIdx);
            SensorData data = generateReading(zone, zone.getMine(), spike);

            try {
                sensorDataService.create(data);
            } catch (Exception e) {
                log.warn("Simulator failed for zone {}: {}", zone.getZoneName(), e.getMessage());
            }
        }
    }

    private SensorData generateReading(Zone zone, Mine mine, boolean spike) {
        SensorData d = new SensorData();
        d.setMine(mine);
        d.setZone(zone);

        if (spike) {
            // Dangerous spike: high methane, low oxygen, high CO
            d.setMethaneLevel(round(1.5 + random.nextDouble() * 3.0));   // 1.5 - 4.5%
            d.setOxygenLevel(round(14.0 + random.nextDouble() * 4.0));   // 14 - 18%
            d.setTemperature(round(38.0 + random.nextDouble() * 12.0));  // 38 - 50°C
            d.setHumidity(round(70.0 + random.nextDouble() * 20.0));     // 70 - 90%
            d.setCarbonMonoxideLevel(round(50.0 + random.nextDouble() * 150.0)); // 50-200ppm
        } else {
            // Normal safe readings with small random variation
            d.setMethaneLevel(round(0.1 + random.nextDouble() * 0.8));   // 0.1 - 0.9%
            d.setOxygenLevel(round(19.5 + random.nextDouble() * 1.5));   // 19.5 - 21%
            d.setTemperature(round(22.0 + random.nextDouble() * 8.0));   // 22 - 30°C
            d.setHumidity(round(40.0 + random.nextDouble() * 30.0));     // 40 - 70%
            d.setCarbonMonoxideLevel(round(random.nextDouble() * 20.0)); // 0 - 20ppm
        }

        d.setAirQualityIndex(round(50.0 + random.nextDouble() * 50.0));
        // riskLevel and riskScore will be set by SensorDataService after ML prediction
        d.setRiskLevel(RiskLevel.SAFE);
        d.setRiskScore(0.0);
        return d;
    }

    private double round(double v) {

        return Math.round(v * 100.0) / 100.0;
    }
}

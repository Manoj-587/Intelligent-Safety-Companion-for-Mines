package com.minesafety.service;

import com.minesafety.entity.Zone;
import com.minesafety.repository.ZoneRepo;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ZoneService {

    private final ZoneRepo repository;

    public ZoneService(ZoneRepo repository) {
        this.repository = repository;
    }

    public Zone create(Zone zone) {
        return repository.save(zone);
    }

    public List<Zone> getAll() {
        return repository.findAll();
    }

    public Zone getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Zone not found"));
    }

    public Zone update(Long id, Zone zone) {
        Zone existing = getById(id);
        zone.setId(existing.getId());
        return repository.save(zone);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }
}

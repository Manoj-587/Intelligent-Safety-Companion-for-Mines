package com.minesafety.service;

import com.minesafety.entity.Alert;
import com.minesafety.repository.AlertRepo;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AlertService {

    private final AlertRepo repository;

    public AlertService(AlertRepo repository) {
        this.repository = repository;
    }

    public Alert create(Alert alert) {
        return repository.save(alert);
    }

    public List<Alert> getAll() {
        return repository.findAll();
    }

    public Alert getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Alert not found"));
    }

    public Alert update(Long id, Alert alert) {
        Alert existing = getById(id);
        alert.setId(existing.getId());
        return repository.save(alert);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }
}

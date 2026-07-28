package com.minesafety.controller;

import com.minesafety.entity.Alert;
import com.minesafety.service.AlertService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/alerts")
@CrossOrigin
public class AlertController {

    private final AlertService service;

    public AlertController(AlertService service) {
        this.service = service;
    }

    @PostMapping
    public Alert create(@RequestBody Alert alert) {
        return service.create(alert);
    }

    @GetMapping
    public List<Alert> getAll(@RequestParam(required = false) Long mineId) {
        return mineId != null ? service.getByMine(mineId) : service.getAll();
    }

    @GetMapping("/by-mine/{mineId}")
    public List<Alert> getByMine(@PathVariable Long mineId) {
        return service.getByMine(mineId);
    }

    @GetMapping("/{id}")
    public Alert getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @PutMapping("/{id}")
    public Alert update(@PathVariable Long id, @RequestBody Alert alert) {
        return service.update(id, alert);
    }

    @DeleteMapping("/{id}")
    public String delete(@PathVariable Long id) {
        service.delete(id);
        return "Alert deleted successfully";
    }
}

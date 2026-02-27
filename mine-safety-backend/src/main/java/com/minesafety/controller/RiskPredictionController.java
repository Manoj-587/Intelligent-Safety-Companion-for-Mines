package com.minesafety.controller;

import com.minesafety.entity.RiskPrediction;
import com.minesafety.service.RiskPredictionService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/risk-predictions")
@CrossOrigin
public class RiskPredictionController {

    private final RiskPredictionService service;

    public RiskPredictionController(RiskPredictionService service) {
        this.service = service;
    }

    @PostMapping
    public RiskPrediction create(@RequestBody RiskPrediction riskPrediction) {
        return service.create(riskPrediction);
    }

    @GetMapping
    public List<RiskPrediction> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public RiskPrediction getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @PutMapping("/{id}")
    public RiskPrediction update(@PathVariable Long id, @RequestBody RiskPrediction riskPrediction) {
        return service.update(id, riskPrediction);
    }

    @DeleteMapping("/{id}")
    public String delete(@PathVariable Long id) {
        service.delete(id);
        return "RiskPrediction deleted successfully";
    }
}

package com.minesafety.controller;

import com.minesafety.entity.Zone;
import com.minesafety.service.ZoneService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/zones")
@CrossOrigin
public class ZoneController {

    private final ZoneService service;

    public ZoneController(ZoneService service) {
        this.service = service;
    }

    @PostMapping
    public Zone create(@RequestBody Zone zone) {
        return service.create(zone);
    }

    @GetMapping
    public List<Zone> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public Zone getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @PutMapping("/{id}")
    public Zone update(@PathVariable Long id, @RequestBody Zone zone) {
        return service.update(id, zone);
    }

    @DeleteMapping("/{id}")
    public String delete(@PathVariable Long id) {
        service.delete(id);
        return "Zone deleted successfully";
    }
}

package com.minesafety.controller;

import com.minesafety.entity.Mine;
import com.minesafety.service.MineService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/mines")
@CrossOrigin
public class MineController {

    private final MineService service;

    public MineController(MineService service) {
        this.service = service;
    }

    @PostMapping
    public Mine create(@RequestBody Mine mine) {
        return service.create(mine);
    }

    @GetMapping
    public List<Mine> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public Mine getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @PutMapping("/{id}")
    public Mine update(@PathVariable Long id, @RequestBody Mine mine) {
        return service.update(id, mine);
    }

    @DeleteMapping("/{id}")
    public String delete(@PathVariable Long id) {
        service.delete(id);
        return "Mine deleted successfully";
    }
}

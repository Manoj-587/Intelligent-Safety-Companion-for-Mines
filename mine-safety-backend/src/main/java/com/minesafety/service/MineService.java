package com.minesafety.service;

import com.minesafety.entity.Mine;
import com.minesafety.repository.MineRepo;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MineService {

    private final MineRepo repository;

    public MineService(MineRepo repository) {
        this.repository = repository;
    }

    public Mine create(Mine mine) {
        return repository.save(mine);
    }

    public List<Mine> getAll() {
        return repository.findAll();
    }

    public Mine getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Mine not found"));
    }

    public Mine update(Long id, Mine mine) {
        Mine existing = getById(id);
        mine.setId(existing.getId());
        return repository.save(mine);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }
}

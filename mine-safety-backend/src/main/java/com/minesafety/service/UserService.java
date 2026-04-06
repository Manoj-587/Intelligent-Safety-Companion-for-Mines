package com.minesafety.service;

import com.minesafety.entity.Mine;
import com.minesafety.entity.User;
import com.minesafety.repository.MineRepo;
import com.minesafety.repository.UserRepo;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    private final UserRepo repository;
    private final MineRepo mineRepo;

    public UserService(UserRepo repository, MineRepo mineRepo) {
        this.repository = repository;
        this.mineRepo = mineRepo;
    }

    public User createUser(User user) {
        return repository.save(user);
    }

    public List<User> getAllUsers() {
        return repository.findAll();
    }

    public User getUserById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public User getUserByEmail(String email) {
        return repository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public User updateUser(Long id, User user) {
        User existing = getUserById(id);
        existing.setFullName(user.getFullName());
        existing.setEmail(user.getEmail());
        if (user.getPhoneNumber() != null) existing.setPhoneNumber(user.getPhoneNumber());
        return repository.save(existing);
    }

    public User assignMine(Long userId, Long mineId) {
        User user = getUserById(userId);
        Mine mine = mineId == null ? null :
            mineRepo.findById(mineId).orElseThrow(() -> new RuntimeException("Mine not found"));
        user.setAssignedMine(mine);
        return repository.save(user);
    }

    public void deleteUser(Long id) {
        repository.deleteById(id);
    }
}
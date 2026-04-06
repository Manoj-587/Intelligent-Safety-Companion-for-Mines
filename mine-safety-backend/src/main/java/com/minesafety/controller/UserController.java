package com.minesafety.controller;

import com.minesafety.entity.User;
import com.minesafety.service.UserService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@CrossOrigin
public class UserController {

    private final UserService service;

    public UserController(UserService service) {
        this.service = service;
    }

    @PostMapping
    public User create(@RequestBody User user) {
        return service.createUser(user);
    }

    @GetMapping
    public List<User> getAll() {
        return service.getAllUsers();
    }

    @GetMapping("/{id}")
    public User getById(@PathVariable Long id) {
        return service.getUserById(id);
    }

    @GetMapping("/by-email")
    public User getByEmail(@RequestParam String email) {
        return service.getUserByEmail(email);
    }

    @PutMapping("/{id}")
    public User update(@PathVariable Long id, @RequestBody User user) {
        return service.updateUser(id, user);
    }

    @PutMapping("/{id}/assign-mine/{mineId}")
    public User assignMine(@PathVariable Long id, @PathVariable Long mineId) {
        return service.assignMine(id, mineId);
    }

    @PutMapping("/{id}/unassign-mine")
    public User unassignMine(@PathVariable Long id) {
        return service.assignMine(id, null);
    }

    @DeleteMapping("/{id}")
    public Map<String, String> delete(@PathVariable Long id) {
        service.deleteUser(id);
        return Map.of("message", "User deleted successfully");
    }
}
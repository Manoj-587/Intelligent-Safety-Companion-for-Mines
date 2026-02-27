package com.minesafety.controller;

import com.minesafety.entity.User;
import com.minesafety.service.UserService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @PutMapping("/{id}")
    public User update(@PathVariable Long id, @RequestBody User user) {
        return service.updateUser(id, user);
    }

    @DeleteMapping("/{id}")
    public String delete(@PathVariable Long id) {
        service.deleteUser(id);
        return "User deleted successfully";
    }
}
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

        System.out.println("USERNAME = " + user.getUsername());
        System.out.println("PASSWORD = " + user.getPassword());
        System.out.println("EMAIL = " + user.getEmail());
        System.out.println("ROLE = " + user.getRole());

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

    @DeleteMapping("/{id}")
    public String delete(@PathVariable Long id) {
        service.deleteUser(id);
        return "User deleted successfully";
    }
}
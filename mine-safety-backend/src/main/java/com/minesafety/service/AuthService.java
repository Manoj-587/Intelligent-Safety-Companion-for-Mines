package com.minesafety.service;

import com.minesafety.dto.AuthResponse;
import com.minesafety.dto.LoginRequest;
import com.minesafety.dto.RegisterRequest;
import com.minesafety.entity.User;
import com.minesafety.enums.Status;
import com.minesafety.repository.UserRepo;
import com.minesafety.util.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepo userRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(UserRepo userRepo, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    public AuthResponse register(RegisterRequest request) {
        if (userRepo.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Email already exists");
        }

        User user = new User();
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setStatus(Status.ACTIVE);

        User saved = userRepo.save(user);

        String token = jwtUtil.generateToken(saved.getEmail());
        return new AuthResponse(saved.getId(), token, saved.getEmail(), saved.getFullName(), saved.getRole().name(),
            saved.getPhoneNumber(), saved.getStatus().name(),
            saved.getAssignedMine() != null ? saved.getAssignedMine().getId() : null,
            saved.getAssignedMine() != null ? saved.getAssignedMine().getMineName() : null);
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepo.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid email or password");
        }

        String token = jwtUtil.generateToken(user.getEmail());
        return new AuthResponse(user.getId(), token, user.getEmail(), user.getFullName(), user.getRole().name(),
            user.getPhoneNumber(), user.getStatus().name(),
            user.getAssignedMine() != null ? user.getAssignedMine().getId() : null,
            user.getAssignedMine() != null ? user.getAssignedMine().getMineName() : null);
    }
}

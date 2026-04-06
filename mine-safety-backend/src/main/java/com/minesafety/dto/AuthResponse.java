package com.minesafety.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthResponse {
    private Long id;
    private String token;
    private String email;
    private String fullName;
    private String role;
    private String phoneNumber;
    private String status;
    private Long assignedMineId;
    private String assignedMineName;
}

package com.minesafety.dto;

import com.minesafety.enums.Role;
import lombok.Data;

@Data
public class RegisterRequest {
    private String fullName;
    private String email;
    private String password;
    private Role role;
    private String phoneNumber;
}

package com.example.sistema_web.dto;

import lombok.Data;

@Data
public class LoginRequest {
    private String username; // email
    private String password;
}
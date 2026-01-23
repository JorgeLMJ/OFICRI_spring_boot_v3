package com.example.sistema_web.dto;

import lombok.Data;

@Data
public class RegistroDTO {
    private String nombre;
    private String email;
    private String password;
    private String rol; // "ADMIN" o "USER"
}
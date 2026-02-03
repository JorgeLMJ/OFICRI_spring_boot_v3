// src/main/java/com/example/sistema_web/dto/AsignacionDosajeDTO.java
package com.example.sistema_web.dto;

import lombok.Data;

@Data
public class AsignacionDosajeDTO {
    private Long id;
    private String area;
    private String cualitativo;
    private String estado;
    private Long documentoId;
    private Long empleadoId;
    private Long emisorId;
}
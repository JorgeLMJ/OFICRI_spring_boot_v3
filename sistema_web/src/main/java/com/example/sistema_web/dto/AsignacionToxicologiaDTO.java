// src/main/java/com/example/sistema_web/dto/AsignacionToxicologiaDTO.java
package com.example.sistema_web.dto;

import lombok.Data;

@Data
public class AsignacionToxicologiaDTO {
    private Long id;
    private String area;
    private String estado;
    private Long documentoId;      // ✅ Debe existir
    private Long empleadoId;       // ✅ Debe existir
    private Long emisorId;         // ✅ Para notificaciones
    private ToxicologiaResultadoDTO resultados = new ToxicologiaResultadoDTO(); // ✅ Objeto completo

    public AsignacionToxicologiaDTO() {}
}
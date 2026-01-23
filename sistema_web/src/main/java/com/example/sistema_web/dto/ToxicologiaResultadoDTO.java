// src/main/java/com/example/sistema_web/dto/ToxicologiaResultadoDTO.java
package com.example.sistema_web.dto;

import lombok.Data;

@Data
public class ToxicologiaResultadoDTO {
    private String marihuana;
    private String cocaina;
    private String benzodiacepinas;
    private String barbituricos;
    private String carbamatos;
    private String estricnina;
    private String cumarinas;
    private String organofosforados;
    private String misoprostol;
    private String piretrinas;

    public ToxicologiaResultadoDTO() {} // ✅ Constructor vacío obligatorio
}
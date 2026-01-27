package com.example.sistema_web.dto;

import lombok.Data;

@Data
public class DocumentoDTO {
    private Long id;
    private String nombresyapellidos;
    private String dni;
    private String edad;
    private String cualitativo;
    private String cuantitativo;
    private String numeroInforme;
    private String nombreOficio;
    private String procedencia;
    private String tipoMuestra;
    private String personaQueConduce;
    private byte[] archivo;
    private Long empleadoId;
}
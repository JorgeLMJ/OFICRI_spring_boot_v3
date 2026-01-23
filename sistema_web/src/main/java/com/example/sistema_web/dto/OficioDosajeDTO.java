// src/main/java/com/example/sistema_web/dto/OficioDosajeDTO.java
package com.example.sistema_web.dto;

import lombok.Data;

@Data
public class OficioDosajeDTO {
    private Long id;
    private String fecha;
    private String nro_oficio;
    private String gradoPNP;
    private String nombresyapellidosPNP;
    private String referencia;
    private String nro_informe;
    private Long documentoId;
    private byte[] archivo;
}
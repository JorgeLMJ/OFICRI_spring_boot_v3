package com.example.sistema_web.dto;
import lombok.Data;
@Data
public class OficioToxicologiaDTO {
    private Long id;
    private String fecha;
    private String nro_oficio;
    private String gradoPNP;
    private String nombresyapellidosPNP;
    private String nro_informe_referencia;
    private Long documentoId;
    private byte[] archivo;
    // ✅ Campos de la tabla Documentos (Inner Join)
    private String personaInvolucrada;
    private String dniInvolucrado;
    private String edadInvolucrado;
    private String tipoMuestra;
    private String nroInformeBase;
}

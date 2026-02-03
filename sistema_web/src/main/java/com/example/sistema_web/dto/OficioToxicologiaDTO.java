package com.example.sistema_web.dto;
import com.example.sistema_web.model.Empleado;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Data;
@Data
public class OficioToxicologiaDTO {
    private Long id;
    private String fecha;
    private String nro_oficio;
    private String gradoPNP;
    private String nombresyapellidosPNP;
    private Long documentoId;
    @ManyToOne
    @JoinColumn(name = "emisor_id")
    private Empleado emisor;
    private byte[] archivo;
    // ✅ Campos de la tabla Documentos (Inner Join)
    private String personaInvolucrada;
    private String dniInvolucrado;
    private String edadInvolucrado;
    private String tipoMuestra;
    private String nroInformeBase;
}

// src/main/java/com/example/sistema_web/model/AsignacionToxicologia.java
package com.example.sistema_web.model;

import com.example.sistema_web.dto.ToxicologiaResultadoDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "asignaciones_toxicologia")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AsignacionToxicologia {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String area;
    private String estado;

    @Lob
    @Column(name = "resultado_toxicologico", columnDefinition = "TEXT")
    private String resultadoToxicologico;

    // --- Relación con Documento ---
    @ManyToOne
    @JoinColumn(name = "documento_id", nullable = false)
    private Documento documento;

    // ✅ NUEVA RELACIÓN: Empleado (Químico Farmacéutico)
    @ManyToOne
    @JoinColumn(name = "empleado_id", nullable = false)
    private Empleado empleado;

    // ✅ Métodos auxiliares para manejar JSON
    public ToxicologiaResultadoDTO getResultados() {
        if (this.resultadoToxicologico == null) {
            return new ToxicologiaResultadoDTO();
        }
        try {
            return new ObjectMapper().readValue(this.resultadoToxicologico, ToxicologiaResultadoDTO.class);
        } catch (Exception e) {
            return new ToxicologiaResultadoDTO();
        }
    }

    public void setResultados(ToxicologiaResultadoDTO resultados) {
        try {
            this.resultadoToxicologico = new ObjectMapper().writeValueAsString(resultados);
        } catch (JsonProcessingException e) {
            this.resultadoToxicologico = "{}";
        }
    }
}
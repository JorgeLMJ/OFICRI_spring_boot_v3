package com.example.sistema_web.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "asignaciones_dosaje")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AsignacionDosaje {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String area;
    private String cualitativo;
    private String estado;

    // 🔗 Relación con Documento
    @ManyToOne
    @JoinColumn(name = "documento_id", nullable = false)
    private Documento documento;

    @ManyToOne
    @JoinColumn(name = "empleado_id")
    private Empleado empleado;

    @ManyToOne
    @JoinColumn(name = "emisor_id")
    private Empleado emisor;
}
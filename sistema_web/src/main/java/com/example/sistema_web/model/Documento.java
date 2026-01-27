package com.example.sistema_web.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "documentos")
@Getter
@Setter
@ToString(exclude = "empleado")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Documento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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
    @Lob
    @Column(name = "archivo", columnDefinition = "LONGBLOB")
    private byte[] archivo;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empleado_id")
    private Empleado empleado;
}
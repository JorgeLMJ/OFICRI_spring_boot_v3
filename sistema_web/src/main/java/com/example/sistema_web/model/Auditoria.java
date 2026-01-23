package com.example.sistema_web.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "auditoria")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Auditoria {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer usuarioId;

    @Column(nullable = false, length = 255)
    private String nombreUsuario;

    @Column(nullable = false, length = 100)
    private String accion; // Ej: "CREAR_EMPLEADO"

    @Column(nullable = false, length = 50)
    private String tipoAccion; // CREATE, UPDATE, DELETE

    @Column(nullable = false, length = 100)
    private String entidad;

    private Integer entidadId;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String detalles;

    @Column(length = 100)
    private String sessionId;

    @Column(length = 45)
    private String ip;

    @Column(columnDefinition = "TEXT")
    private String userAgent;

    @Column(nullable = false)
    private LocalDateTime fecha = LocalDateTime.now();
}
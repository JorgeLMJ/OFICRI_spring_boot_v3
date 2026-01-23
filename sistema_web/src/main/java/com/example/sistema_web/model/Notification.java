// Notification.java
package com.example.sistema_web.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 500)
    private String message;

    @Column(nullable = false, length = 50)
    private String area;

    @Column(name = "asignacion_id")
    private Long asignacionId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime timestamp;

    // ✅ CAMBIA ESTO
    @Column(name = "is_read", nullable = false, columnDefinition = "TINYINT(1) DEFAULT 0")
    private Boolean read = false; // 👈 "read", no "isRead"

    // Notification.java
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empleado_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Empleado empleado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "emisor_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Empleado emisor;

    public Notification(String message, String area, Long asignacionId, Empleado destinatario, Empleado emisor) {
        this.message = message;
        this.area = area;
        this.asignacionId = asignacionId;
        this.empleado = destinatario;
        this.emisor = emisor;
        this.timestamp = LocalDateTime.now();
        this.read = false; // 👈 Usa "read"
    }
}
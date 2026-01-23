// src/main/java/com/example/sistema_web/model/OficioDosaje.java
package com.example.sistema_web.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "oficio_dosaje")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OficioDosaje {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String fecha;
    private String nro_oficio;
    private String gradoPNP;
    private String nombresyapellidosPNP;
    private String referencia;
    private String nro_informe;
    @Lob
    @Column(name = "archivo", columnDefinition = "LONGBLOB")
    private byte[] archivo;
    // Relación con Documento
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "documento_id", nullable = false)
    private Documento documento;
}
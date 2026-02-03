package com.example.sistema_web.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "oficio_dosaje")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@ToString(exclude = "documento")
public class OficioDosaje {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String fecha;
    private String nro_oficio;
    private String gradoPNP;
    private String nombresyapellidosPNP;
    @ManyToOne
    @JoinColumn(name = "emisor_id")
    private Empleado emisor;
    @Lob
    @Column(name = "archivo", columnDefinition = "LONGBLOB")
    private byte[] archivo;
    // Relación con Documento
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "documento_id", nullable = false)
    private Documento documento;
}
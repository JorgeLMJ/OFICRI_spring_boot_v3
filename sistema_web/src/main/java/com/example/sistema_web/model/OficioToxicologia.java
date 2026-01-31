package com.example.sistema_web.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "oficio_toxicologia")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@ToString(exclude = "documento")
public class OficioToxicologia {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String fecha;
    private String nro_oficio;
    private String gradoPNP;
    private String nombresyapellidosPNP;
    private String nro_informe_referencia;
    @Lob
    @Column(name = "archivo", columnDefinition = "LONGBLOB")
    private byte[] archivo;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "documento_id", nullable = false)
    private Documento documento;
}

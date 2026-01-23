package com.example.sistema_web.dto;

public class GraficoEstadoDTO {
    private String estado;
    private Long cantidad;

    public GraficoEstadoDTO(String estado, Long cantidad) {
        this.estado = estado;
        this.cantidad = cantidad;
    }

    public String getEstado() { return estado; }
    public Long getCantidad() { return cantidad; }
}
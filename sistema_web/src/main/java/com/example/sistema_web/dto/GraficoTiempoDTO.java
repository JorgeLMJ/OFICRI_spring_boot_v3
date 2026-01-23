package com.example.sistema_web.dto;

public class GraficoTiempoDTO {
    private String tipoAnalisis;
    private Double tiempoPromedioHoras;

    public GraficoTiempoDTO(String tipoAnalisis, Double tiempoPromedioHoras) {
        this.tipoAnalisis = tipoAnalisis;
        this.tiempoPromedioHoras = tiempoPromedioHoras;
    }

    public String getTipoAnalisis() { return tipoAnalisis; }
    public Double getTiempoPromedioHoras() { return tiempoPromedioHoras; }
}
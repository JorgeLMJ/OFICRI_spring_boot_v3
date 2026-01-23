package com.example.sistema_web.dto;

public class GraficoSustanciaDTO {
    private String sustancia;
    private Long totalAnalisis;

    public GraficoSustanciaDTO(String sustancia, Long totalAnalisis) {
        this.sustancia = sustancia;
        this.totalAnalisis = totalAnalisis;
    }

    public String getSustancia() { return sustancia; }
    public Long getTotalAnalisis() { return totalAnalisis; }
}
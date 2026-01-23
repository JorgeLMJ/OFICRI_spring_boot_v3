// src/main/java/com/example/sistema_web/dto/ReporteDTO.java
package com.example.sistema_web.dto;

import lombok.Data;

public class ReporteDTO {

    @Data
    public static class SustanciaDTO {
        private String sustancia;
        private Long totalAnalisis;
        public SustanciaDTO(String sustancia, Long totalAnalisis) {
            this.sustancia = sustancia;
            this.totalAnalisis = totalAnalisis;
        }
    }

    @Data
    public static class EstadoDTO {
        private String estado;
        private Long cantidad;
        public EstadoDTO(String estado, Long cantidad) {
            this.estado = estado;
            this.cantidad = cantidad;
        }
    }

    @Data
    public static class DocumentoDiaDTO {
        private String fecha;
        private Long total;
        public DocumentoDiaDTO(String fecha, Long total) {
            this.fecha = fecha;
            this.total = total;
        }
    }

    @Data
    public static class EmpleadoDTO {
        private String empleado;
        private String apellido;
        private Long totalAnalisis;
        public EmpleadoDTO(String empleado, String apellido, Long totalAnalisis) {
            this.empleado = empleado;
            this.apellido = apellido;
            this.totalAnalisis = totalAnalisis;
        }
    }

    // ✅ NUEVA CLASE
    @Data
    public static class Metrica {
        private Long totalEmpleados;
        private Long totalDocumentos;
        public Metrica(Long totalEmpleados, Long totalDocumentos) {
            this.totalEmpleados = totalEmpleados;
            this.totalDocumentos = totalDocumentos;
        }
    }
    @Data
    public static class RangoCualitativoDTO {
        private String rango;
        private long cantidad;

        public RangoCualitativoDTO(String rango, long cantidad) {
            this.rango = rango;
            this.cantidad = cantidad;
        }

        public String getRango() { return rango; }
        public long getCantidad() { return cantidad; }
    }
}
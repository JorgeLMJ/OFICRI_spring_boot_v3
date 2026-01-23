package com.example.sistema_web.dto;

public class GraficoEmpleadoDTO {
    private String empleado;
    private String apellido;
    private Long totalAnalisis;

    public GraficoEmpleadoDTO(String empleado, String apellido, Long totalAnalisis) {
        this.empleado = empleado;
        this.apellido = apellido;
        this.totalAnalisis = totalAnalisis;
    }

    public String getEmpleado() { return empleado; }
    public String getApellido() { return apellido; }
    public Long getTotalAnalisis() { return totalAnalisis; }
}
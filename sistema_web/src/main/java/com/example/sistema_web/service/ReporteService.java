// src/main/java/com/example/sistema_web/service/ReporteService.java
package com.example.sistema_web.service;

import com.example.sistema_web.dto.ReporteDTO;
import java.io.IOException;
import java.util.List;

public interface ReporteService {
    Long getTotalEmpleados();
    Long getTotalDocumentos();
    List<ReporteDTO.SustanciaDTO> getAnalisisPorSustancia();
    List<ReporteDTO.EstadoDTO> getEstados();
    List<ReporteDTO.DocumentoDiaDTO> getDocumentosPorDia();
    List<ReporteDTO.EmpleadoDTO> getEmpleadosProductividad();
    byte[] generarExcel(int mes, int año) throws IOException;
    byte[] generarPdf(int mes, int año) throws IOException;
    List<ReporteDTO.RangoCualitativoDTO> getRangosCualitativos();
}
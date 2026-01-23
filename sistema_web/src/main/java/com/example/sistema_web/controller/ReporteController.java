// src/main/java/com/example/sistema_web/controller/ReporteController.java
package com.example.sistema_web.controller;

import com.example.sistema_web.dto.*;
import com.example.sistema_web.service.ReporteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/reportes")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class ReporteController {

    private final ReporteService reporteService;

    // 📊 Métricas generales
    @GetMapping("/metricas")
    public ResponseEntity<ReporteDTO.Metrica> getMetricas() {
        Long empleados = reporteService.getTotalEmpleados();
        Long documentos = reporteService.getTotalDocumentos();
        ReporteDTO.Metrica metrica = new ReporteDTO.Metrica(empleados, documentos);
        return ResponseEntity.ok(metrica);
    }

    // 📊 Análisis por sustancia
    @GetMapping("/analisis/sustancia")
    public ResponseEntity<List<ReporteDTO.SustanciaDTO>> getAnalisisPorSustancia() {
        return ResponseEntity.ok(reporteService.getAnalisisPorSustancia());
    }

    // 📊 Estados (Positivo/Negativo)
    @GetMapping("/estados")
    public ResponseEntity<List<ReporteDTO.EstadoDTO>> getEstados() {
        return ResponseEntity.ok(reporteService.getEstados());
    }

    // 📊 Documentos por día (últimos 7 días)
    @GetMapping("/documentos/dia")
    public ResponseEntity<List<ReporteDTO.DocumentoDiaDTO>> getDocumentosPorDia() {
        return ResponseEntity.ok(reporteService.getDocumentosPorDia());
    }

    // 📊 Productividad por empleado
    @GetMapping("/empleados/productividad")
    public ResponseEntity<List<ReporteDTO.EmpleadoDTO>> getEmpleadosProductividad() {
        return ResponseEntity.ok(reporteService.getEmpleadosProductividad());
    }

    // 📥 Exportación
    @GetMapping(value = "/excel", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<byte[]> descargarExcel(
            @RequestParam(defaultValue = "12") int mes,
            @RequestParam(defaultValue = "2025") int año) throws IOException {

        byte[] excelData = reporteService.generarExcel(mes, año);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDispositionFormData("attachment", "reporte_" + año + "_" + mes + ".xlsx");
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        return new ResponseEntity<>(excelData, headers, HttpStatus.OK);
    }

    @GetMapping(value = "/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> descargarPdf(
            @RequestParam(defaultValue = "12") int mes,
            @RequestParam(defaultValue = "2025") int año) throws IOException {

        byte[] pdfData = reporteService.generarPdf(mes, año);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDispositionFormData("attachment", "reporte_" + año + "_" + mes + ".pdf");
        headers.setContentType(MediaType.APPLICATION_PDF);
        return new ResponseEntity<>(pdfData, headers, HttpStatus.OK);
    }
    @GetMapping("/rangos/cualitativo")
    public ResponseEntity<List<ReporteDTO.RangoCualitativoDTO>> getRangosCualitativos() {
        return ResponseEntity.ok(reporteService.getRangosCualitativos());
    }
}
package com.example.sistema_web.controller;

import com.example.sistema_web.model.Auditoria;
import com.example.sistema_web.service.AuditoriaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/auditoria")
@CrossOrigin(origins = "http://localhost:4200")
public class AuditoriaController {

    @Autowired
    private AuditoriaService auditoriaService;

    // Solo lectura: listar todas las auditorías (solo para administradores)
    @GetMapping
    public ResponseEntity<List<Auditoria>> getAllAuditorias() {
        try {
            List<Auditoria> auditorias = auditoriaService.getAllAuditorias();
            return ResponseEntity.ok(auditorias);
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }

    // Filtrar por tipo de acción (opcional, pero útil)
    @GetMapping("/tipo/{tipoAccion}")
    public ResponseEntity<List<Auditoria>> getAuditoriasByTipo(@PathVariable String tipoAccion) {
        try {
            List<Auditoria> auditorias = auditoriaService.getAuditoriasByTipo(tipoAccion);
            return ResponseEntity.ok(auditorias);
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }

    // Filtrar por rango de fechas (opcional)
    @GetMapping("/rango")
    public ResponseEntity<List<Auditoria>> getAuditoriasPorRango(
            @RequestParam String fechaInicio,
            @RequestParam String fechaFin) {
        try {
            List<Auditoria> auditorias = auditoriaService.getAuditoriasPorRango(fechaInicio, fechaFin);
            return ResponseEntity.ok(auditorias);
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }
}
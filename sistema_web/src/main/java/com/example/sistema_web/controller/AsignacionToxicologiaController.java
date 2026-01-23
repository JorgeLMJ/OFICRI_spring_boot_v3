package com.example.sistema_web.controller;

import com.example.sistema_web.dto.AsignacionToxicologiaDTO;
import com.example.sistema_web.service.AsignacionToxicologiaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/asignaciones-toxicologia")
@RequiredArgsConstructor
public class AsignacionToxicologiaController {

    private final AsignacionToxicologiaService service;

    @PostMapping
    public ResponseEntity<AsignacionToxicologiaDTO> crear(@RequestBody AsignacionToxicologiaDTO dto) {
        return ResponseEntity.ok(service.crear(dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AsignacionToxicologiaDTO> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(service.obtenerPorId(id));
    }

    @GetMapping
    public ResponseEntity<List<AsignacionToxicologiaDTO>> listar() {
        return ResponseEntity.ok(service.listar());
    }

    @PutMapping("/{id}")
    public ResponseEntity<AsignacionToxicologiaDTO> actualizar(@PathVariable Long id, @RequestBody AsignacionToxicologiaDTO dto) {
        return ResponseEntity.ok(service.actualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id); // ✅ Usa el mismo servicio
        return ResponseEntity.noContent().build();
    }
}
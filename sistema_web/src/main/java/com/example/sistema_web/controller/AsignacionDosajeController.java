package com.example.sistema_web.controller;

import com.example.sistema_web.dto.AsignacionDosajeDTO;
import com.example.sistema_web.service.AsignacionDosajeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/asignaciones-dosaje")
@RequiredArgsConstructor
public class AsignacionDosajeController {
    private final AsignacionDosajeService service;

    @PostMapping
    public ResponseEntity<AsignacionDosajeDTO> crear(@RequestBody AsignacionDosajeDTO dto) {
        return ResponseEntity.ok(service.crear(dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AsignacionDosajeDTO> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(service.obtenerPorId(id));
    }

    @GetMapping
    public ResponseEntity<List<AsignacionDosajeDTO>> listar() {
        return ResponseEntity.ok(service.listar());
    }

    @PutMapping("/{id}")
    public ResponseEntity<AsignacionDosajeDTO> actualizar(@PathVariable Long id, @RequestBody AsignacionDosajeDTO dto) {
        return ResponseEntity.ok(service.actualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}

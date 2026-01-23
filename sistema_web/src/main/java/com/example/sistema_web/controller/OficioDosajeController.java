package com.example.sistema_web.controller;

import com.example.sistema_web.dto.OficioDosajeDTO;
import com.example.sistema_web.service.OficioDosajeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/oficio-dosaje")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class OficioDosajeController {

    private final OficioDosajeService service;

    @PostMapping
    public ResponseEntity<OficioDosajeDTO> crear(@RequestBody OficioDosajeDTO dto) {
        return ResponseEntity.ok(service.crear(dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OficioDosajeDTO> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(service.obtenerPorId(id));
    }

    @GetMapping
    public ResponseEntity<List<OficioDosajeDTO>> listar() {
        return ResponseEntity.ok(service.listar());
    }

    @PutMapping("/{id}")
    public ResponseEntity<OficioDosajeDTO> actualizar(@PathVariable Long id, @RequestBody OficioDosajeDTO dto) {
        return ResponseEntity.ok(service.actualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> descargarArchivo(@PathVariable Long id) {
        try {
            byte[] contenido = service.obtenerContenidoArchivo(id);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=oficio_dosaje.docx")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(contenido);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @GetMapping("/{id}/editor-config")
    public ResponseEntity<Map<String, Object>> getConfig(@PathVariable Long id, @RequestParam(defaultValue = "edit") String mode) {
        return ResponseEntity.ok(service.getEditorConfig(id, mode));
    }

    @PostMapping("/{id}/callback")
    public ResponseEntity<Map<String, Object>> callback(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Map<String, Object> response = new HashMap<>();
        try {
            // Status 2: El documento está listo para ser guardado
            if (body.get("status").toString().equals("2")) {
                service.actualizarDesdeOnlyOffice(id, body.get("url").toString());
            }
            response.put("error", 0);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("error", 1);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
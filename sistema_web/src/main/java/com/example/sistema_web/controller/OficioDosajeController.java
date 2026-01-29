package com.example.sistema_web.controller;
import com.example.sistema_web.config.JwtAuthFilter;
import com.example.sistema_web.dto.OficioDosajeDTO;
import com.example.sistema_web.service.OficioDosajeService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
   // private static final String DOCKER_HOST = "spring-boot-container";

    // 1. Crear Oficio
    @PostMapping("/nuevo")
    public ResponseEntity<Long> iniciarNuevoOficioDosaje() {
        Long nuevoId = service.crearOficioDosajeVacio();
        System.out.println("🆕 Nuevo oficio dosaje creado con ID: " + nuevoId);
        return ResponseEntity.ok(nuevoId);
    }

    // 2. CONFIG EDITOR (Con validación, SIN crear basura)
    @GetMapping("/{id}/editor-config")
    public ResponseEntity<Map<String, Object>> getEditorConfig(
            @PathVariable Long id,
            @RequestParam(defaultValue = "edit") String mode) {

        if (!service.existeOficioDosaje(id)) {
            return ResponseEntity.notFound().build();
        }


        Map<String, Object> config = new HashMap<>();
        config.put("documentType", "word");
        config.put("width", "100%");
        config.put("height", "100%");

        Map<String, Object> document = new HashMap<>();
        document.put("fileType", "docx");
        // ✅ Key dinámica para evitar caché
        document.put("key", "oficio-" + id + "-" + System.currentTimeMillis());
        document.put("title", "Oficio_Dosaje_" + id + ".docx");
        document.put("url", "http://host.docker.internal:8080/api/oficio-dosaje/" + id + "/download");

        Map<String, Object> editorConfig = new HashMap<>();
        editorConfig.put("mode", mode);
        editorConfig.put("lang", "es");
        editorConfig.put("callbackUrl", "http://host.docker.internal:8080/api/oficio-dosaje/" + id + "/save-callback");

        config.put("document", document);
        config.put("editorConfig", editorConfig);
        String token = Jwts.builder()
                .setClaims(config) // Metemos toda la config dentro del token
                .signWith(SignatureAlgorithm.HS256, "eFT7MiNho7WgLlP54slEHxlvriVduD4I".getBytes())
                .compact();

        config.put("token", token);
        return ResponseEntity.ok(config);
    }

    // 3. Descargar archivo (Usado por OnlyOffice y el usuario)
    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> downloadOficioDosaje(@PathVariable Long id) {
        byte[] data = service.obtenerContenidoArchivo(id);
        ByteArrayResource resource = new ByteArrayResource(data);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"oficio_dosaje.docx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                .contentLength(data.length)
                .body(resource);
    }

    // 4. Callback de Guardado (OnlyOffice)
    @PostMapping("/{id}/save-callback")
    public ResponseEntity<Map<String, Object>> saveCallback(
            @PathVariable Long id,
            @RequestBody Map<String, Object> payload,
            @RequestParam(required = false) Long documentoId) {

        Integer status = (Integer) payload.get("status");

        // 👇 IMPRIMIR EL STATUS PARA DEPURAR
        System.out.println("📨 Callback recibido para ID " + id + " | Status: " + status);

        // ✅ CORRECCIÓN: Aceptamos Status 6 (Force Save) Y Status 2 (Cierre del editor)
        if (status != null && (status == 2 || status == 6)) {
            String urlDescarga = (String) payload.get("url");

            // Solo intentamos guardar si hay una URL de descarga válida
            if (urlDescarga != null && !urlDescarga.isEmpty()) {
                System.out.println("💾 Guardando cambios (Status " + status + ") para Oficio ID: " + id);
                service.actualizarDesdeUrlOnlyOffice(id, urlDescarga, documentoId);
            } else {
                System.out.println("⚠️ Status " + status + " recibido pero sin URL de descarga (posiblemente sin cambios).");
            }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("error", 0);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<OficioDosajeDTO> crear(@RequestBody OficioDosajeDTO dto) {
        return ResponseEntity.ok(service.crear(dto));
    }

    @GetMapping
    public ResponseEntity<List<OficioDosajeDTO>> listar() {
        return ResponseEntity.ok(service.listar());
    }

    @DeleteMapping("/{id}/cancelar")
    public ResponseEntity<Void> cancelarEdicion(@PathVariable Long id) {
        service.eliminar(id);
        System.out.println("🗑️ Borrador ID " + id + " eliminado por cancelación del usuario.");
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarOficioDosaje(@PathVariable Long id) {
        try {
            service.eliminar(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/upload-directo")
    public ResponseEntity<String> subirArchivoManual(
            @PathVariable Long id,
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        try {
            // Convertimos el archivo recibido a bytes
            byte[] contenido = file.getBytes();

            // Llamamos a tu servicio (que ya tiene este método uploadDocumento)
            service.uploadOficioDosaje(id, contenido);

            return ResponseEntity.ok("✅ Archivo recibido y guardado en BD. Tamaño: " + contenido.length + " bytes.");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("❌ Error al subir: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<OficioDosajeDTO> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(service.obtenerPorId(id));
    }


    @PutMapping("/{id}")
    public ResponseEntity<OficioDosajeDTO> actualizar(@PathVariable Long id, @RequestBody OficioDosajeDTO dto) {
        return ResponseEntity.ok(service.actualizar(id, dto));
    }
    @PostMapping("/{id}/sincronizar")
    public ResponseEntity<?> sincronizar(@PathVariable Long id) {
        System.out.println("🔄 Solicitud de sincronización recibida para ID: " + id);
        service.sincronizarDatosAlWord(id);
        return ResponseEntity.ok(Map.of("mensaje", "Sincronización completada"));
    }
}
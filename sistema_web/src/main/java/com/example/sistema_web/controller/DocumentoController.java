package com.example.sistema_web.controller;

import com.example.sistema_web.config.JwtAuthFilter;
import com.example.sistema_web.dto.DocumentoDTO;
import com.example.sistema_web.service.DocumentoService;
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
@RequestMapping("/api/documentos")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class DocumentoController {

    private final DocumentoService service;
    // ✅ 1. CREAR NUEVO (Asignando el empleado de inmediato)
    @PostMapping("/nuevo")
    public ResponseEntity<Long> iniciarNuevoDocumento() {
        // Obtenemos el ID del empleado que tiene la sesión activa
        Long empleadoIdActual = JwtAuthFilter.getCurrentEmpleadoId(); //

        // Pasamos el ID al servicio para que el documento no sea "huérfano"
        Long nuevoId = service.crearDocumentoVacio(empleadoIdActual);

        System.out.println("🆕 Documento " + nuevoId + " creado por Empleado ID: " + empleadoIdActual);
        return ResponseEntity.ok(nuevoId);
    }

    // ✅ 2. CONFIG EDITOR (Corregido: Se restauró la variable editorConfig)
    @GetMapping("/{id}/editor-config")
    public ResponseEntity<Map<String, Object>> getEditorConfig(
            @PathVariable Long id,
            @RequestParam(defaultValue = "edit") String mode) {

        if (!service.existeDocumento(id)) {
            System.out.println("⛔ Intento de acceso a ID inexistente: " + id);
            return ResponseEntity.notFound().build();
        }

        // 1. Obtener nombre dinámico y ID del empleado actual
        String nombreArchivo = service.obtenerNombreSugerido(id);
        Long empleadoIdActual = JwtAuthFilter.getCurrentEmpleadoId();

        // 2. Configuración del Objeto Document
        Map<String, Object> config = new HashMap<>();
        config.put("documentType", "word");

        Map<String, Object> document = new HashMap<>();
        document.put("fileType", "docx");
        document.put("key", "doc-" + id + "-" + System.currentTimeMillis());
        document.put("title", nombreArchivo);
        document.put("url", "http://host.docker.internal:8080/api/documentos/" + id + "/download");

        // 3. Configuración del Editor (Lo que faltaba)
        Map<String, Object> editorConfig = new HashMap<>();
        editorConfig.put("mode", mode);
        editorConfig.put("lang", "es");

        // URL a donde OnlyOffice enviará los cambios al guardar
        String callbackUrl = "http://host.docker.internal:8080/api/documentos/" + id + "/save-callback";
        if (empleadoIdActual != null) {
            callbackUrl += "?empleadoId=" + empleadoIdActual;
        }
        editorConfig.put("callbackUrl", callbackUrl);

        // Datos del usuario para que aparezca quién edita en OnlyOffice
        Map<String, String> user = new HashMap<>();
        user.put("id", empleadoIdActual != null ? empleadoIdActual.toString() : "anon");
        user.put("name", "Usuario " + (empleadoIdActual != null ? empleadoIdActual : "Invitado"));
        editorConfig.put("user", user);

        // 4. Unir todo en el mapa principal
        config.put("document", document);
        config.put("editorConfig", editorConfig);

        // 5. Generar Token JWT para OnlyOffice
        String token = Jwts.builder()
                .setClaims(config)
                .signWith(SignatureAlgorithm.HS256, "eFT7MiNho7WgLlP54slEHxlvriVduD4I".getBytes())
                .compact();

        config.put("token", token);
        return ResponseEntity.ok(config);
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> downloadDocumento(@PathVariable Long id) {
        byte[] data = service.obtenerContenidoArchivo(id);
        ByteArrayResource resource = new ByteArrayResource(data);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"documento.docx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                .contentLength(data.length)
                .body(resource);
    }

    @PostMapping("/{id}/save-callback")
    public ResponseEntity<Map<String, Object>> saveCallback(
            @PathVariable Long id,
            @RequestBody Map<String, Object> payload,
            @RequestParam(required = false) Long empleadoId) {

        Integer status = (Integer) payload.get("status");

        // 👇 IMPRIMIR EL STATUS PARA DEPURAR
        System.out.println("📨 Callback recibido para ID " + id + " | Status: " + status);

        // ✅ CORRECCIÓN: Aceptamos Status 6 (Force Save) Y Status 2 (Cierre del editor)
        if (status != null && (status == 2 || status == 6)) {
            String urlDescarga = (String) payload.get("url");

            // Solo intentamos guardar si hay una URL de descarga válida
            if (urlDescarga != null && !urlDescarga.isEmpty()) {
                System.out.println("💾 Guardando cambios (Status " + status + ") para Doc ID: " + id);
                service.actualizarDesdeUrlOnlyOffice(id, urlDescarga, empleadoId);
            } else {
                System.out.println("⚠️ Status " + status + " recibido pero sin URL de descarga (posiblemente sin cambios).");
            }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("error", 0);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<DocumentoDTO> crear(@RequestBody DocumentoDTO dto) {
        return ResponseEntity.ok(service.crear(dto));
    }
    @GetMapping
    public ResponseEntity<List<DocumentoDTO>> listar() {
        return ResponseEntity.ok(service.listar());
    }
    @DeleteMapping("/{id}/cancelar")
    public ResponseEntity<Void> cancelarEdicion(@PathVariable Long id) {
        service.eliminar(id);
        System.out.println("🗑️ Borrador ID " + id + " eliminado por cancelación del usuario.");
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")  // 👈 Esta anotación es la clave
    public ResponseEntity<?> eliminarDocumento(@PathVariable Long id) {
        try {
            service.eliminar(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }
    // Endpoint para probar subida directa desde Postman
    @PostMapping("/{id}/upload-directo")
    public ResponseEntity<String> subirArchivoManual(
            @PathVariable Long id,
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        try {
            // Convertimos el archivo recibido a bytes
            byte[] contenido = file.getBytes();

            // Llamamos a tu servicio (que ya tiene este método uploadDocumento)
            service.uploadDocumento(id, contenido);

            return ResponseEntity.ok("✅ Archivo recibido y guardado en BD. Tamaño: " + contenido.length + " bytes.");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("❌ Error al subir: " + e.getMessage());
        }
    }
    @GetMapping("/{id}")
    public ResponseEntity<DocumentoDTO> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(service.obtenerPorId(id));
    }

    @PostMapping("/{id}/actualizar-tag")
    public ResponseEntity<?> actualizarTag(
            @PathVariable Long id,
            @RequestBody Map<String, String> payload) {

        String tag = payload.get("tag");   // Ej: "CUANTITATIVO"
        String valor = payload.get("valor"); // Ej: "0.55"

        service.actualizarTagEnWord(id, tag, valor);

        return ResponseEntity.ok(Map.of("mensaje", "Word actualizado correctamente"));
    }

}
package com.example.sistema_web.service;

import com.example.sistema_web.config.JwtAuthFilter;
import com.example.sistema_web.dto.AsignacionToxicologiaDTO;
import com.example.sistema_web.dto.ToxicologiaResultadoDTO;
import com.example.sistema_web.model.AsignacionToxicologia;
import com.example.sistema_web.model.Documento;
import com.example.sistema_web.model.Empleado;
import com.example.sistema_web.repository.AsignacionToxicologiaRepository;
import com.example.sistema_web.repository.DocumentoRepository;
import com.example.sistema_web.repository.EmpleadoRepository;
import org.apache.poi.xwpf.usermodel.*;
import lombok.RequiredArgsConstructor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AsignacionToxicologiaServiceImpl implements AsignacionToxicologiaService {

    private final AsignacionToxicologiaRepository repository;
    private final DocumentoRepository documentoRepository;
    private final EmpleadoRepository empleadoRepository;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public AsignacionToxicologiaDTO crear(AsignacionToxicologiaDTO dto) {
        Documento doc = documentoRepository.findById(dto.getDocumentoId())
                .orElseThrow(() -> new RuntimeException("Documento no encontrado"));

        Empleado emisor = empleadoRepository.findById(dto.getEmisorId())
                .orElseThrow(() -> new RuntimeException("Emisor no encontrado"));

        Empleado destinatario = empleadoRepository.findById(dto.getEmpleadoId())
                .orElseThrow(() -> new RuntimeException("Empleado destinatario no encontrado"));

        AsignacionToxicologia asignacion = AsignacionToxicologia.builder()
                .area(dto.getArea())
                .estado("EN_PROCESO")
                .documento(doc)
                .empleado(destinatario) // ✅ Este es el perito seleccionado en el modal
                .emisor(emisor)
                .build();

        asignacion.setResultados(dto.getResultados());
        AsignacionToxicologia saved = repository.save(asignacion);

        sincronizarDatosAlWord(saved.getId());

        // ✅ MODIFICADO: Solo enviamos a la persona asignada
        enviarNotificacionIndividual(saved, emisor, destinatario);

        return mapToDTO(saved);
    }

    // ✅ NUEVO MÉTODO OPTIMIZADO: Envío a una sola persona
    private void enviarNotificacionIndividual(AsignacionToxicologia saved, Empleado emisor, Empleado destinatario) {
        if (destinatario != null && emisor != null) {
            String mensaje = emisor.getNombre() + " " + emisor.getApellido() +
                    " le ha asignado la tarea de toxicología ID " + saved.getId() + ".";

            // Creamos una única notificación dirigida al ID del perito seleccionado
            notificationService.crearNotificacion(mensaje, "Toxicología", saved.getId(), destinatario, emisor);

            System.out.println("🔔 Notificación enviada únicamente a: " + destinatario.getNombre() + " (ID: " + destinatario.getId() + ")");
        }
    }

    @Override
    @Transactional
    public AsignacionToxicologiaDTO actualizar(Long id, AsignacionToxicologiaDTO dto) {
        AsignacionToxicologia asignacion = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Asignación no encontrada"));

        asignacion.setEstado(dto.getEstado());
        asignacion.setResultados(dto.getResultados());

        AsignacionToxicologia updated = repository.save(asignacion);

        // Sincronización automática al actualizar
        sincronizarDatosAlWord(updated.getId());

        return mapToDTO(updated);
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        if (!repository.existsById(id)) {
            throw new RuntimeException("ID no encontrado para eliminar");
        }
        repository.deleteById(id);
    }

    @Override
    public AsignacionToxicologiaDTO obtenerPorId(Long id) {
        return repository.findById(id).map(this::mapToDTO)
                .orElseThrow(() -> new RuntimeException("Asignación no encontrada"));
    }

    @Override
    public List<AsignacionToxicologiaDTO> listar() {
        // 1. Obtener el ID del empleado logueado desde el Token
        Long idLogueado = JwtAuthFilter.getCurrentEmpleadoId();

        // Caso SuperAdmin (usuario base sin empleado asociado)
        if (idLogueado == null) {
            return repository.findAll().stream().map(this::mapToDTO).collect(Collectors.toList());
        }

        Empleado empLogueado = empleadoRepository.findById(idLogueado).orElse(null);
        if (empLogueado == null) {
            return repository.findAll().stream().map(this::mapToDTO).collect(Collectors.toList());
        }

        String cargo = empLogueado.getCargo().toLowerCase().trim();
        List<AsignacionToxicologia> listaFinal;

        // 🛡️ REGLA DE VISIBILIDAD POR EMPLEADO_ID (PARA QUÍMICOS)
        if (cargo.contains("admin")) {
            // El Administrador sigue viendo TODO
            System.out.println("🔓 ACCESO TOTAL - Administrador: " + empLogueado.getNombre());
            listaFinal = repository.findAll();
        }
        else if (cargo.contains("quimico") || cargo.contains("químico")) {
            // 🔒 Los Químicos SOLO ven los trabajos donde ellos son el PERITO ASIGNADO (empleado_id)
            System.out.println("🔒 ACCESO PRIVADO (Por Asignación) - Químico: " + empLogueado.getNombre());
            listaFinal = repository.findByEmpleadoId(idLogueado);
        }
        else {
            // Los Auxiliares ven lo que ellos mismos REGISTRARON (emisor_id)
            System.out.println("🔒 ACCESO PRIVADO (Por Creación) - Auxiliar: " + empLogueado.getNombre());
            listaFinal = repository.findByEmisorId(idLogueado);
        }

        return listaFinal.stream()
                .map(this::mapToDTO)
                .sorted((a, b) -> b.getId().compareTo(a.getId())) // Más nuevos primero
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void sincronizarDatosAlWord(Long id) {
        // 1. Obtener la asignación y el documento
        AsignacionToxicologia asignacion = repository.findById(id).orElseThrow();
        Documento docBase = asignacion.getDocumento();

        if (docBase == null || docBase.getArchivo() == null) return;

        try (ByteArrayInputStream bis = new ByteArrayInputStream(docBase.getArchivo());
             XWPFDocument doc = new XWPFDocument(bis)) {

            // --- 1. FILTRADO DE DATOS (Con .trim() para seguridad) ---
            Map<String, String> activas = filtrarSustanciasActivas(asignacion.getResultados());

            List<String> positivos = activas.entrySet().stream()
                    .filter(e -> e.getValue() != null && e.getValue().trim().equalsIgnoreCase("POSITIVO"))
                    .map(Map.Entry::getKey).collect(Collectors.toList());

            List<String> negativos = activas.entrySet().stream()
                    .filter(e -> e.getValue() != null && e.getValue().trim().equalsIgnoreCase("NEGATIVO"))
                    .map(Map.Entry::getKey).collect(Collectors.toList());

            System.out.println("DEBUG WORD -> ID: " + id + " | Positivos: " + positivos.size() + " | Negativos: " + negativos.size());

            // --- 2. LLENADO DE TABLA (Busca por "EXAMEN") ---
            XWPFTable tablaExamen = null;
            for (XWPFTable table : doc.getTables()) {
                // Buscamos la tabla que tenga "EXAMEN" en la primera celda
                if (!table.getRows().isEmpty() && table.getRow(0).getCell(0).getText().toUpperCase().contains("EXAMEN")) {
                    tablaExamen = table;
                    break;
                }
            }

            if (tablaExamen != null) {
                // Borrar filas viejas (dejamos solo la cabecera, índice 0)
                while (tablaExamen.getRows().size() > 1) {
                    tablaExamen.removeRow(1);
                }
                // Llenar nuevas filas
                for (String s : positivos) agregarFilaTabla(tablaExamen, s, "POSITIVO");
                for (String s : negativos) agregarFilaTabla(tablaExamen, s, "NEGATIVO");
            }

            // --- 3. GENERAR TEXTO DE CONCLUSIÓN ---
            String conclusion = redactarTextoConclusiones(positivos, negativos);

            // --- 4. REEMPLAZO INTELIGENTE (LA SOLUCIÓN) ---
            // Buscamos la etiqueta O el inicio del texto antiguo para sobrescribirlo
            String marcadorEtiqueta = "$c_resultado";
            String textoAntiguoParcial = "-- En la muestra";

            boolean reemplazado = false;

            // A) Buscar en Párrafos normales
            for (XWPFParagraph p : doc.getParagraphs()) {
                String textoP = p.getText();
                // 💡 AQUÍ ESTÁ EL TRUCO: Busca la variable O el texto viejo
                if (textoP != null && (textoP.contains(marcadorEtiqueta) || textoP.contains(textoAntiguoParcial))) {
                    sobrescribirParrafoCompleto(p, conclusion);
                    reemplazado = true;
                }
            }

            // B) Buscar dentro de Tablas (por si el texto está en una celda)
            if (!reemplazado) {
                for (XWPFTable tbl : doc.getTables()) {
                    for (XWPFTableRow row : tbl.getRows()) {
                        for (XWPFTableCell cell : row.getTableCells()) {
                            for (XWPFParagraph p : cell.getParagraphs()) {
                                String textoP = p.getText();
                                if (textoP != null && (textoP.contains(marcadorEtiqueta) || textoP.contains(textoAntiguoParcial))) {
                                    sobrescribirParrafoCompleto(p, conclusion);
                                }
                            }
                        }
                    }
                }
            }

            // --- 5. GUARDAR ---
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            doc.write(bos);
            docBase.setArchivo(bos.toByteArray());
            documentoRepository.save(docBase);

            System.out.println("✅ Sincronización finalizada exitosamente.");

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error crítico Word: " + e.getMessage());
        }
    }

    private void agregarFilaTabla(XWPFTable tabla, String sustancia, String resultado) {
        XWPFTableRow row = tabla.createRow();
        row.getCell(0).setText(sustancia.toUpperCase());
        XWPFRun run = row.getCell(1).getParagraphs().get(0).createRun();
        run.setText(resultado);
        run.setBold(true);
        run.setFontFamily("Times New Roman");
        run.setFontSize(11);
    }
    private void sobrescribirParrafoCompleto(XWPFParagraph p, String nuevoTexto) {
        // 1. Borrar todo el contenido actual del párrafo (runs)
        for (int i = p.getRuns().size() - 1; i >= 0; i--) {
            p.removeRun(i);
        }

        // 2. Crear un nuevo run con el texto correcto
        // (Opcional: Si necesitas negritas parciales, usa tu lógica de split aquí)
        XWPFRun run = p.createRun();
        run.setText(nuevoTexto);
        run.setFontFamily("Times New Roman");
        run.setFontSize(12);

        System.out.println("✏️ Párrafo actualizado con nuevo texto.");
    }
    private String redactarTextoConclusiones(List<String> pos, List<String> neg) {
        if (pos.isEmpty() && neg.isEmpty()) {
            return "-- En la muestra M-1 analizada se obtuvo un resultado: No se detectaron sustancias de interés toxicológico.";
        }
        StringBuilder sb = new StringBuilder("-- En la muestra M-1 analizada se obtuvo un resultado: ");
        if (!pos.isEmpty()) {
            sb.append("POSITIVO para presencia de ").append(String.join(", ", pos)); // Quitamos ** para simplificar este run directo
            if (!neg.isEmpty()) sb.append(" y ");
            else sb.append(".");
        }
        if (!neg.isEmpty()) {
            sb.append("NEGATIVO para presencia de ").append(String.join(", ", neg)).append(".");
        }
        return sb.toString();
    }

    private Map<String, String> filtrarSustanciasActivas(ToxicologiaResultadoDTO res) {
        Map<String, String> map = new HashMap<>();
        if (res.getMarihuana() != null) map.put("Cannabilones (Marihuana)", res.getMarihuana());
        if (res.getCocaina() != null) map.put("Alcaloide de cocaína", res.getCocaina());
        if (res.getBenzodiacepinas() != null) map.put("Benzodiacepinas", res.getBenzodiacepinas());
        if (res.getBarbituricos() != null) map.put("Barbitúricos", res.getBarbituricos());
        if (res.getCarbamatos() != null) map.put("Carbamatos", res.getCarbamatos());
        if (res.getEstricnina() != null) map.put("Estricnina", res.getEstricnina());
        if (res.getOrganofosforados() != null) map.put("Organofosforado", res.getOrganofosforados());
        if (res.getMisoprostol() != null) map.put("Misoprostol", res.getMisoprostol());
        if (res.getPiretrinas() != null) map.put("Piretrinas", res.getPiretrinas());
        if (res.getCumarinas() != null) map.put("Cumarinas", res.getCumarinas());
        return map;
    }
    private AsignacionToxicologiaDTO mapToDTO(AsignacionToxicologia asignacion) {
        AsignacionToxicologiaDTO dto = new AsignacionToxicologiaDTO();
        dto.setId(asignacion.getId());
        dto.setArea(asignacion.getArea());
        dto.setEstado(asignacion.getEstado());
        if (asignacion.getDocumento() != null) dto.setDocumentoId(asignacion.getDocumento().getId());
        if (asignacion.getEmpleado() != null) dto.setEmpleadoId(asignacion.getEmpleado().getId());
        dto.setResultados(asignacion.getResultados());
        return dto;
    }
}
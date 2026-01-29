package com.example.sistema_web.service;

import com.example.sistema_web.dto.AsignacionToxicologiaDTO;
import com.example.sistema_web.dto.ToxicologiaResultadoDTO;
import com.example.sistema_web.model.AsignacionToxicologia;
import com.example.sistema_web.model.Documento;
import com.example.sistema_web.model.Empleado;
import com.example.sistema_web.repository.AsignacionToxicologiaRepository;
import com.example.sistema_web.repository.DocumentoRepository;
import com.example.sistema_web.repository.EmpleadoRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
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
                .orElseThrow(() -> new RuntimeException("Documento no encontrado con ID: " + dto.getDocumentoId()));

        Empleado destinatario = empleadoRepository.findById(dto.getEmpleadoId())
                .orElseThrow(() -> new RuntimeException("Empleado no encontrado con ID: " + dto.getEmpleadoId()));

        Empleado emisor = empleadoRepository.findById(dto.getEmisorId())
                .orElseThrow(() -> new RuntimeException("Emisor no encontrado con ID: " + dto.getEmisorId()));

        String estado = "EN_PROCESO";
        if ("COMPLETADO".equalsIgnoreCase(dto.getEstado())) {
            estado = "COMPLETADO";
        }

        AsignacionToxicologia asignacion = AsignacionToxicologia.builder()
                .area(dto.getArea())
                .estado(estado)
                .documento(doc)
                .empleado(destinatario)
                .build();

        asignacion.setResultados(dto.getResultados());
        AsignacionToxicologia saved = repository.save(asignacion);

        // ✅ Sincronizar tabla en Word
        generarTablaSustanciasEnWord(dto.getDocumentoId(), dto.getResultados());

        // ✅ Notificación
        if ("EN_PROCESO".equals(estado)) {
            enviarNotificacion(saved, emisor);
        }

        return mapToDTO(saved);
    }

    @Override
    @Transactional
    public AsignacionToxicologiaDTO actualizar(Long id, AsignacionToxicologiaDTO dto) {
        AsignacionToxicologia asignacion = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Asignación no encontrada con ID: " + id));

        String estado = "COMPLETADO".equalsIgnoreCase(dto.getEstado()) ? "COMPLETADO" : "EN_PROCESO";
        asignacion.setEstado(estado);

        if (dto.getDocumentoId() != null) {
            Documento doc = documentoRepository.findById(dto.getDocumentoId()).orElseThrow();
            asignacion.setDocumento(doc);
        }

        asignacion.setResultados(dto.getResultados());
        AsignacionToxicologia updated = repository.save(asignacion);

        // ✅ Actualizar tabla en Word con los nuevos resultados
        generarTablaSustanciasEnWord(updated.getDocumento().getId(), dto.getResultados());

        return mapToDTO(updated);
    }

    private void generarTablaSustanciasEnWord(Long documentoId, ToxicologiaResultadoDTO resultados) {
        Documento doc = documentoRepository.findById(documentoId).orElseThrow();
        if (doc.getArchivo() == null) return;

        try (ByteArrayInputStream bis = new ByteArrayInputStream(doc.getArchivo());
             XWPFDocument document = new XWPFDocument(bis);
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {

            XWPFTable tablaDestino = null;

            // 🔍 Búsqueda mejorada: Buscamos la tabla que tenga la palabra "Sustancia" o "Resultado"
            for (XWPFTable tbl : document.getTables()) {
                if (!tbl.getRows().isEmpty()) {
                    // Accedemos a la primera fila (encabezado)
                    XWPFTableRow headerRow = tbl.getRow(0);

                    // Concatenamos el texto de todas las celdas de la primera fila para validar
                    String textoEncabezado = headerRow.getTableCells().stream()
                            .map(XWPFTableCell::getText)
                            .collect(Collectors.joining(" "))
                            .toUpperCase();

                    if (textoEncabezado.contains("EXAMEN") || textoEncabezado.contains("RESULTADO DEL ANALISIS")) {
                        tablaDestino = tbl;
                        break;
                    }
                }
            }
            if (tablaDestino != null) {
                // 🧹 Limpieza: Eliminamos todas las filas excepto el encabezado (fila 0)
                int filasTotales = tablaDestino.getRows().size();
                for (int i = filasTotales - 1; i > 0; i--) {
                    tablaDestino.removeRow(i);
                }

                Map<String, String> activas = filtrarSustanciasActivas(resultados);

                // ✍️ Si no hay sustancias, podrías poner una fila indicándolo
                if (activas.isEmpty()) {
                    XWPFTableRow row = tablaDestino.createRow();
                    row.getCell(0).setText("NINGUNA");
                    row.getCell(1).setText("NEGATIVO");
                } else {
                    for (Map.Entry<String, String> entry : activas.entrySet()) {
                        XWPFTableRow row = tablaDestino.createRow();
                        row.getCell(0).setText(entry.getKey());
                        row.getCell(1).setText(entry.getValue());
                    }
                }

                document.write(bos);
                doc.setArchivo(bos.toByteArray());
                documentoRepository.save(doc);
            }
        } catch (IOException e) {
            throw new RuntimeException("Error procesando Word: " + e.getMessage());
        }
    }

    private Map<String, String> filtrarSustanciasActivas(ToxicologiaResultadoDTO res) {
        Map<String, String> map = new HashMap<>();
        if (res.getMarihuana() != null) map.put("MARIHUANA", res.getMarihuana());
        if (res.getCocaina() != null) map.put("COCAÍNA", res.getCocaina());
        if (res.getBenzodiacepinas() != null) map.put("BENZODIACEPINAS", res.getBenzodiacepinas());
        if (res.getBarbituricos() != null) map.put("BARBITÚRICOS", res.getBarbituricos());
        if (res.getCarbamatos() != null) map.put("CARBAMATOS", res.getCarbamatos());
        if (res.getEstricnina() != null) map.put("ESTRICNINA", res.getEstricnina());
        if (res.getOrganofosforados() != null) map.put("ORGANOFOSFORADOS", res.getOrganofosforados());
        if (res.getMisoprostol() != null) map.put("MISOPROSTOL", res.getMisoprostol());
        if (res.getPiretrinas() != null) map.put("PIRETRINAS", res.getPiretrinas());
        if (res.getCumarinas() != null) map.put("CUMARINAS", res.getCumarinas());
        return map;
    }

    private void enviarNotificacion(AsignacionToxicologia saved, Empleado emisor) {
        Empleado quimico = empleadoRepository.findByCargo("Quimico Farmaceutico");
        if (quimico != null) {
            String mensaje = emisor.getNombre() + " " + emisor.getApellido() +
                    " ha asignado la tarea de toxicología ID " + saved.getId() + ".";
            notificationService.crearNotificacion(mensaje, "Toxicología", saved.getId(), quimico, emisor);
        }
    }

    @Override
    public AsignacionToxicologiaDTO obtenerPorId(Long id) {
        return repository.findById(id).map(this::mapToDTO).orElseThrow();
    }

    @Override
    public List<AsignacionToxicologiaDTO> listar() {
        return repository.findAll().stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    @Override
    public void eliminar(Long id) {
        repository.deleteById(id);
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
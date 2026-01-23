// src/main/java/com/example/sistema_web/service/AsignacionToxicologiaServiceImpl.java
package com.example.sistema_web.service;

import com.example.sistema_web.dto.AsignacionToxicologiaDTO;
import com.example.sistema_web.model.AsignacionToxicologia;
import com.example.sistema_web.model.Documento;
import com.example.sistema_web.model.Empleado;
import com.example.sistema_web.repository.AsignacionToxicologiaRepository;
import com.example.sistema_web.repository.DocumentoRepository;
import com.example.sistema_web.repository.EmpleadoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AsignacionToxicologiaServiceImpl implements AsignacionToxicologiaService {

    private final AsignacionToxicologiaRepository repository;
    private final DocumentoRepository documentoRepository;
    private final EmpleadoRepository empleadoRepository;
    private final NotificationService notificationService;

    @Override
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
        // ✅ Establecer resultados DESPUÉS del build
        asignacion.setResultados(dto.getResultados());
        AsignacionToxicologia saved = repository.save(asignacion);

        // ✅ Notificación SOLO si estado = "EN_PROCESO"
        if ("EN_PROCESO".equals(estado)) {
            Empleado quimico = empleadoRepository.findByCargo("Quimico Farmaceutico");
            if (quimico != null) {
                String mensaje = emisor.getNombre() + " " + emisor.getApellido() +
                        " ha asignado la tarea de toxicología ID " + saved.getId() + ".";
                notificationService.crearNotificacion(mensaje, "Toxicología", saved.getId(), quimico, emisor);
            }
        }

        return mapToDTO(saved);
    }

    @Override
    public AsignacionToxicologiaDTO obtenerPorId(Long id) {
        return repository.findById(id)
                .map(this::mapToDTO)
                .orElseThrow(() -> new RuntimeException("Asignación no encontrada con ID: " + id));
    }

    @Override
    public List<AsignacionToxicologiaDTO> listar() {
        return repository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public AsignacionToxicologiaDTO actualizar(Long id, AsignacionToxicologiaDTO dto) {
        AsignacionToxicologia asignacion = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Asignación no encontrada con ID: " + id));

        String estado = "EN_PROCESO";
        if ("COMPLETADO".equalsIgnoreCase(dto.getEstado())) {
            estado = "COMPLETADO";
        }
        asignacion.setEstado(estado);

        if (dto.getDocumentoId() != null) {
            Documento doc = documentoRepository.findById(dto.getDocumentoId())
                    .orElseThrow(() -> new RuntimeException("Documento no encontrado con ID: " + dto.getDocumentoId()));
            asignacion.setDocumento(doc);
        }

        if (dto.getEmpleadoId() != null) {
            Empleado destinatario = empleadoRepository.findById(dto.getEmpleadoId())
                    .orElseThrow(() -> new RuntimeException("Empleado no encontrado con ID: " + dto.getEmpleadoId()));
            asignacion.setEmpleado(destinatario);
        }

        asignacion.setResultados(dto.getResultados());

        AsignacionToxicologia updated = repository.save(asignacion);

        // ✅ Notificación SOLO si estado = "EN_PROCESO"
        if ("EN_PROCESO".equals(estado)) {
            Empleado emisor = (dto.getEmisorId() != null)
                    ? empleadoRepository.findById(dto.getEmisorId()).orElse(asignacion.getEmpleado())
                    : asignacion.getEmpleado();

            Empleado quimico = empleadoRepository.findByCargo("Quimico Farmaceutico");
            if (quimico != null && emisor != null) {
                String mensaje = emisor.getNombre() + " " + emisor.getApellido() +
                        " ha asignado la tarea de toxicología ID " + updated.getId() + ".";
                notificationService.crearNotificacion(mensaje, "Toxicología", updated.getId(), quimico, emisor);
            }
        }

        return mapToDTO(updated);
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
        if (asignacion.getDocumento() != null) {
            dto.setDocumentoId(asignacion.getDocumento().getId());
        }
        if (asignacion.getEmpleado() != null) {
            dto.setEmpleadoId(asignacion.getEmpleado().getId());
        }
        dto.setResultados(asignacion.getResultados());
        return dto;
    }
}
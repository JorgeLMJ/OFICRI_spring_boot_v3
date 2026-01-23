package com.example.sistema_web.service;

import com.example.sistema_web.dto.AsignacionDosajeDTO;
import com.example.sistema_web.model.AsignacionDosaje;
import com.example.sistema_web.model.Documento;
import com.example.sistema_web.model.Empleado;
import com.example.sistema_web.repository.AsignacionDosajeRepository;
import com.example.sistema_web.repository.DocumentoRepository;
import com.example.sistema_web.repository.EmpleadoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AsignacionDosajeServiceImpl implements AsignacionDosajeService {

    private final AsignacionDosajeRepository repository;
    private final DocumentoRepository documentoRepository;
    private final EmpleadoRepository empleadoRepository;
    private final NotificationService notificationService;
    private final DocumentoService documentoService; // ✅ Inyectado correctamente

    @Override
    @Transactional
    public AsignacionDosajeDTO crear(AsignacionDosajeDTO dto) {
        Documento doc = documentoRepository.findById(dto.getDocumentoId())
                .orElseThrow(() -> new RuntimeException("Documento no encontrado con ID: " + dto.getDocumentoId()));

        Empleado destinatario = empleadoRepository.findById(dto.getEmpleadoId())
                .orElseThrow(() -> new RuntimeException("Empleado no encontrado con ID: " + dto.getEmpleadoId()));

        Empleado emisor = empleadoRepository.findById(dto.getEmisorId())
                .orElseThrow(() -> new RuntimeException("Emisor no encontrado con ID: " + dto.getEmisorId()));

        String estado = "COMPLETADO".equalsIgnoreCase(dto.getEstado()) ? "COMPLETADO" : "EN_PROCESO";

        AsignacionDosaje asignacion = AsignacionDosaje.builder()
                .area(dto.getArea())
                .cualitativo(dto.getCualitativo())
                .estado(estado)
                .documento(doc)
                .empleado(destinatario)
                .build();

        AsignacionDosaje saved = repository.save(asignacion);

        // ✅ MAGIA: Si se crea como completado o tiene valor, actualizamos el Word
        this.verificarYActualizarWord(dto);

        if ("EN_PROCESO".equals(estado)) {
            enviarNotificacionAsignacion(emisor, saved.getId());
        }

        return mapToDTO(saved);
    }

    @Override
    @Transactional
    public AsignacionDosajeDTO actualizar(Long id, AsignacionDosajeDTO dto) {
        AsignacionDosaje asignacion = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Asignación no encontrada con ID: " + id));

        asignacion.setArea(dto.getArea());
        asignacion.setCualitativo(dto.getCualitativo());

        String estado = "COMPLETADO".equalsIgnoreCase(dto.getEstado()) ? "COMPLETADO" : "EN_PROCESO";
        asignacion.setEstado(estado);

        if (dto.getDocumentoId() != null) {
            Documento doc = documentoRepository.findById(dto.getDocumentoId()).orElseThrow();
            asignacion.setDocumento(doc);
        }

        if (dto.getEmpleadoId() != null) {
            Empleado destinatario = empleadoRepository.findById(dto.getEmpleadoId()).orElseThrow();
            asignacion.setEmpleado(destinatario);
        }

        AsignacionDosaje updated = repository.save(asignacion);

        // ✅ MAGIA: Actualizamos el Word físicamente con el nuevo valor
        this.verificarYActualizarWord(dto);

        if ("EN_PROCESO".equals(estado)) {
            Empleado emisor = (dto.getEmisorId() != null) ?
                    empleadoRepository.findById(dto.getEmisorId()).orElse(null) : null;
            enviarNotificacionAsignacion(emisor, updated.getId());
        }

        return mapToDTO(updated);
    }

    // --- MÉTODOS DE APOYO ---

    private void verificarYActualizarWord(AsignacionDosajeDTO dto) {
        if (dto.getDocumentoId() != null && dto.getCualitativo() != null && !dto.getCualitativo().isEmpty()) {
            System.out.println("🔄 Inyectando resultado en Word vía DocumentoService...");
            documentoService.actualizarCampoEnWord(
                    dto.getDocumentoId(),
                    "CUANTITATIVO",
                    dto.getCualitativo()
            );
        }
    }

    private void enviarNotificacionAsignacion(Empleado emisor, Long id) {
        Empleado quimico = empleadoRepository.findByCargo("Quimico Farmaceutico");
        if (quimico != null && emisor != null) {
            String mensaje = emisor.getNombre() + " " + emisor.getApellido() +
                    " ha asignado la tarea de dosaje ID " + id + ".";
            notificationService.crearNotificacion(mensaje, "Dosaje", id, quimico, emisor);
        }
    }

    @Override
    public AsignacionDosajeDTO obtenerPorId(Long id) {
        return repository.findById(id).map(this::mapToDTO).orElseThrow();
    }

    @Override
    public List<AsignacionDosajeDTO> listar() {
        return repository.findAll().stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    @Override
    public void eliminar(Long id) {
        repository.deleteById(id);
    }

    private AsignacionDosajeDTO mapToDTO(AsignacionDosaje asignacion) {
        AsignacionDosajeDTO dto = new AsignacionDosajeDTO();
        dto.setId(asignacion.getId());
        dto.setArea(asignacion.getArea());
        dto.setCualitativo(asignacion.getCualitativo());
        dto.setEstado(asignacion.getEstado());
        if (asignacion.getDocumento() != null) dto.setDocumentoId(asignacion.getDocumento().getId());
        if (asignacion.getEmpleado() != null) dto.setEmpleadoId(asignacion.getEmpleado().getId());
        return dto;
    }
}
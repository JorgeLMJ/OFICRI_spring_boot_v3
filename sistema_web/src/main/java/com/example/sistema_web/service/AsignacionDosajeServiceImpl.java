package com.example.sistema_web.service;

import com.example.sistema_web.config.JwtAuthFilter;
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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AsignacionDosajeServiceImpl implements AsignacionDosajeService {

    private final AsignacionDosajeRepository repository;
    private final DocumentoRepository documentoRepository;
    private final EmpleadoRepository empleadoRepository;
    private final NotificationService notificationService;
    private final DocumentoService documentoService;

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
                .emisor(emisor)
                .build();

        AsignacionDosaje saved = repository.save(asignacion);

        // ✅ Sincronización automática
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

        // ✅ Sincronización automática
        this.verificarYActualizarWord(dto);

        if ("EN_PROCESO".equals(estado)) {
            Empleado emisor = (dto.getEmisorId() != null) ?
                    empleadoRepository.findById(dto.getEmisorId()).orElse(null) : null;
            enviarNotificacionAsignacion(emisor, updated.getId());
        }

        return mapToDTO(updated);
    }

    // ✅ IMPLEMENTACIÓN DEL MÉTODO FALTANTE PARA CORREGIR EL ERROR
    @Override
    @Transactional
    public void sincronizarDatosAlWord(Long id) {
        AsignacionDosaje asignacion = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("No se encontró la asignación para sincronizar"));

        if (asignacion.getDocumento() != null && asignacion.getCualitativo() != null) {
            documentoService.actualizarCampoEnWord(
                    asignacion.getDocumento().getId(),
                    "CUANTITATIVO",
                    asignacion.getCualitativo()
            );
            System.out.println("✅ Word sincronizado para Dosaje ID: " + id);
        }
    }

    private void verificarYActualizarWord(AsignacionDosajeDTO dto) {
        if (dto.getDocumentoId() != null && dto.getCualitativo() != null && !dto.getCualitativo().isEmpty()) {
            documentoService.actualizarCampoEnWord(
                    dto.getDocumentoId(),
                    "CUANTITATIVO",
                    dto.getCualitativo()
            );
        }
    }

    private void enviarNotificacionAsignacion(Empleado emisor, Long id) {
        // ✅ CORRECCIÓN: Buscamos una LISTA de químicos, no solo uno
        List<Empleado> quimicos = empleadoRepository.findAllByCargo("Quimico Farmaceutico");

        if (quimicos != null && !quimicos.isEmpty() && emisor != null) {
            String mensaje = emisor.getNombre() + " " + emisor.getApellido() +
                    " ha asignado la tarea de dosaje ID " + id + ".";

            // Enviamos la notificación a cada químico registrado
            for (Empleado q : quimicos) {
                notificationService.crearNotificacion(mensaje, "Dosaje", id, q, emisor);
            }
        }
    }

    @Override
    public AsignacionDosajeDTO obtenerPorId(Long id) {
        return repository.findById(id).map(this::mapToDTO).orElseThrow();
    }

    @Override
    public List<AsignacionDosajeDTO> listar() {
        // 1. Obtener el ID del empleado logueado desde el Token
        Long idLogueado = JwtAuthFilter.getCurrentEmpleadoId();

        if (idLogueado == null) {
            System.err.println("⚠️ No se detectó ID de empleado en la sesión.");
            return new ArrayList<>();
        }

        // 2. Buscar sus datos para verificar su rango/cargo
        Empleado empLogueado = empleadoRepository.findById(idLogueado).orElse(null);
        if (empLogueado == null) return new ArrayList<>();

        String cargo = empLogueado.getCargo().trim().toLowerCase();
        List<AsignacionDosaje> listaFinal;

        // 🛡️ REGLA DE VISIBILIDAD: Admin y Químicos ven todo el laboratorio
        if (cargo.contains("admin") || cargo.contains("quimico")) {
            System.out.println("🔓 Acceso TOTAL Dosaje para: " + empLogueado.getNombre());
            listaFinal = repository.findAll();
        } else {
            // Los Auxiliares ven solo sus propias asignaciones (donde emisor_id = su ID)
            System.out.println("🔒 Acceso FILTRADO Dosaje para: " + empLogueado.getNombre());
            listaFinal = repository.findByEmisorId(idLogueado);
        }

        return listaFinal.stream()
                .map(this::mapToDTO)
                .sorted((a, b) -> b.getId().compareTo(a.getId())) // Más recientes arriba
                .collect(Collectors.toList());
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
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
                .empleado(destinatario) // ✅ El perito seleccionado en el formulario
                .emisor(emisor)
                .build();

        AsignacionDosaje saved = repository.save(asignacion);

        // Sincronización automática
        this.verificarYActualizarWord(dto);

        // ✅ MODIFICADO: Envío de notificación solo al destinatario asignado
        if ("EN_PROCESO".equals(estado)) {
            enviarNotificacionIndividual(emisor, destinatario, saved.getId());
        }

        return mapToDTO(saved);
    }

    // ✅ NUEVO MÉTODO OPTIMIZADO
    private void enviarNotificacionIndividual(Empleado emisor, Empleado destinatario, Long asignacionId) {
        if (emisor != null && destinatario != null) {
            String mensaje = emisor.getNombre() + " " + emisor.getApellido() +
                    " le ha asignado la tarea de dosaje ID " + asignacionId + ".";

            // Se crea un único registro en la tabla 'notifications' dirigido al ID del destinatario
            notificationService.crearNotificacion(mensaje, "Dosaje", asignacionId, destinatario, emisor);

            System.out.println("🔔 Notificación de Dosaje enviada exclusivamente a: " +
                    destinatario.getNombre() + " (ID: " + destinatario.getId() + ")");
        }
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

            // 🚩 CORRECCIÓN AQUÍ: Se agregaron los 3 argumentos requeridos
            // Se envía: emisor, el empleado asignado (destinatario) y el ID
            enviarNotificacionIndividual(emisor, updated.getEmpleado(), updated.getId());
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


    @Override
    public AsignacionDosajeDTO obtenerPorId(Long id) {
        return repository.findById(id).map(this::mapToDTO).orElseThrow();
    }

    @Override
    public List<AsignacionDosajeDTO> listar() {
        Long idLogueado = JwtAuthFilter.getCurrentEmpleadoId();
        if (idLogueado == null) {
            System.out.println("👑 Acceso SuperAdmin detectado por ID nulo (Sistema Base). Listando todo.");
            return repository.findAll().stream().map(this::mapToDTO).collect(Collectors.toList());
        }

        Empleado empLogueado = empleadoRepository.findById(idLogueado).orElse(null);
        if (empLogueado == null) return repository.findAll().stream().map(this::mapToDTO).collect(Collectors.toList());
        String cargo = empLogueado.getCargo().toLowerCase();
        List<AsignacionDosaje> listaFinal;

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
                .sorted((a, b) -> b.getId().compareTo(a.getId()))
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
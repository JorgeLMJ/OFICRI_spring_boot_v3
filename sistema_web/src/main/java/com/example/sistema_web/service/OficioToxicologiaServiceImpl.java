package com.example.sistema_web.service;

import com.example.sistema_web.dto.OficioToxicologiaDTO;
import com.example.sistema_web.model.Documento;
import com.example.sistema_web.model.OficioToxicologia;
import com.example.sistema_web.repository.DocumentoRepository;
import com.example.sistema_web.repository.OficioToxicologiaRepository;
import fr.opensagres.xdocreport.document.IXDocReport;
import fr.opensagres.xdocreport.document.registry.XDocReportRegistry;
import fr.opensagres.xdocreport.template.IContext;
import fr.opensagres.xdocreport.template.TemplateEngineKind;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OficioToxicologiaServiceImpl implements OficioToxicologiaService{

    private final OficioToxicologiaRepository repository;
    private final DocumentoRepository documentoRepository;

    // ✅ 1. CREAR OFICIO
    @Override
    @Transactional
    public Long crearOficioToxicologiaVacio() {
        OficioToxicologia oficio = new OficioToxicologia();
        OficioToxicologia saved = repository.save(oficio);
        return saved.getId();
    }

    // ✅ 2. VALIDAR EXISTENCIA (Sin crear nada)
    @Override
    public boolean existeOficioToxicologia(Long id) {
        return repository.existsById(id);
    }

    // ✅ 3. OBTENER ARCHIVO (Si es nuevo, devuelve plantilla)
    @Override
    public byte[] obtenerContenidoArchivo(Long id) {
        OficioToxicologia oficio = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Oficio no encontrado"));
        // SI YA EXISTE UN ARCHIVO GUARDADO (Usuario ya presionó Guardar antes)
        if (oficio.getArchivo() != null && oficio.getArchivo().length > 0) {
            return oficio.getArchivo();
        }
        // SI ES NUEVO: Leemos la plantilla de resources y la devolvemos SIN hacer repository.save()
        try {
            Resource resource = new ClassPathResource("templates/oficio_dosaje.docx");
            if (!resource.exists()) {
                throw new RuntimeException("❌ Plantilla no encontrada en resources");
            }
            return resource.getInputStream().readAllBytes();
        } catch (Exception e) {
            throw new RuntimeException("Error al leer la plantilla de oficio", e);
        }
    }

    // ✅ 4. GUARDAR DESDE ONLYOFFICE (Con extracción de datos)
    @Override
    @Transactional
    public void actualizarDesdeUrlOnlyOffice(Long id, String urlDescarga, Long documentoId) {
        if (urlDescarga.contains("localhost")) {
            urlDescarga = urlDescarga.replace("localhost", "onlyoffice_server");
        }

        System.out.println("⬇️ Descargando cambios del Oficio desde OnlyOffice: " + urlDescarga);
        try {
            java.net.URL url = new java.net.URL(urlDescarga);
            byte[] archivoBytes;
            java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
            try (java.io.InputStream in = connection.getInputStream()) {
                archivoBytes = in.readAllBytes();
            }

            OficioToxicologia oficio = repository.findById(id).orElseThrow(() ->
                    new RuntimeException("Oficio no encontrado con ID: " + id)
            );

            oficio.setArchivo(archivoBytes);

            if (documentoId != null) {
                documentoRepository.findById(documentoId).ifPresent(oficio::setDocumento);
            }

            repository.save(oficio);
            System.out.println("✅ ¡OFICIO GUARDADO EN MYSQL! Tamaño: " + archivoBytes.length + " bytes.");

        } catch (Exception e) {
            System.err.println("❌ ERROR AL GUARDAR OFICIO: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Error al guardar archivo: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public OficioToxicologiaDTO crear(OficioToxicologiaDTO dto) {
        OficioToxicologia oficio = mapToEntity(dto);
        byte[] plantillaBase = cargarPlantillaDesdeResources();
        oficio.setArchivo(plantillaBase);

        OficioToxicologia saved = repository.save(oficio);
        return mapToDTO(saved);
    }

    private byte[] cargarPlantillaDesdeResources() {
        try {
            Resource resource = new ClassPathResource("templates/oficio_toxicologia.docx");
            return resource.getInputStream().readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException("Error al leer la plantilla de toxicologia", e);
        }
    }

    @Override
    public OficioToxicologiaDTO obtenerPorId(Long id) {
        return repository.findById(id).map(this::mapToDTO)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Oficio no encontrado"));
    }

    @Override
    public List<OficioToxicologiaDTO> listar() {
        return repository.findAll().stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    @Override
    public OficioToxicologiaDTO actualizar(Long id, OficioToxicologiaDTO dto) {
        OficioToxicologia oficio = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Oficio no encontrado"));

        oficio.setFecha(dto.getFecha());
        oficio.setNro_oficio(dto.getNro_oficio());
        oficio.setGradoPNP(dto.getGradoPNP());
        oficio.setNombresyapellidosPNP(dto.getNombresyapellidosPNP());
        oficio.setNro_informe_referencia(dto.getNro_informe_referencia());
        oficio.setArchivo(dto.getArchivo());
        if (dto.getDocumentoId() != null) {
            var documento = documentoRepository.findById(dto.getDocumentoId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Documento no encontrado"));
            oficio.setDocumento(documento);
        }
        return mapToDTO(repository.save(oficio));
    }

    @Override
    public void eliminar(Long id) {
        if (!repository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "ID no encontrado");
        }
        repository.deleteById(id);
    }

    @Override
    public void uploadOficioToxicologia(Long id, byte[] archivoBytes) {
        OficioToxicologia oficio = repository.findById(id).orElseThrow();
        oficio.setArchivo(archivoBytes);
        repository.save(oficio);
    }


    // --- MAPPER ---
    private OficioToxicologiaDTO mapToDTO(OficioToxicologia oficio) {
        OficioToxicologiaDTO dto = new OficioToxicologiaDTO();
        dto.setId(oficio.getId());
        dto.setFecha(oficio.getFecha());
        dto.setNro_oficio(oficio.getNro_oficio());
        dto.setGradoPNP(oficio.getGradoPNP());
        dto.setNombresyapellidosPNP(oficio.getNombresyapellidosPNP());
        dto.setNro_informe_referencia(oficio.getNro_informe_referencia());
        dto.setArchivo(oficio.getArchivo());
        if (oficio.getDocumento() != null) {
            dto.setDocumentoId(oficio.getDocumento().getId());
            dto.setPersonaInvolucrada(oficio.getDocumento().getNombresyapellidos());
            dto.setDniInvolucrado(oficio.getDocumento().getDni());
            dto.setEdadInvolucrado(oficio.getDocumento().getEdad());
            dto.setTipoMuestra(oficio.getDocumento().getTipoMuestra());
            dto.setNroInformeBase(oficio.getDocumento().getNumeroInforme());
        }
        return dto;
    }

    private OficioToxicologia mapToEntity(OficioToxicologiaDTO dto) {
        OficioToxicologia.OficioToxicologiaBuilder builder = OficioToxicologia.builder()
                .fecha(dto.getFecha())
                .nro_oficio(dto.getNro_oficio())
                .gradoPNP(dto.getGradoPNP())
                .nombresyapellidosPNP(dto.getNombresyapellidosPNP())
                .nro_informe_referencia(dto.getNro_informe_referencia())
                .archivo(dto.getArchivo());

        if (dto.getDocumentoId() != null) {
            var documento = documentoRepository.findById(dto.getDocumentoId()).orElseThrow();
            builder.documento(documento);
        }
        return builder.build();
    }

    @Override
    @Transactional
    public void sincronizarDatosAlWord(Long id) {
        OficioToxicologia oficio = repository.findById(id).orElseThrow();
        Documento docBase = oficio.getDocumento();

        try {
            // 1. DETERMINAR LA FUENTE DEL DOCUMENTO
            InputStream in;
            if (oficio.getArchivo() != null && oficio.getArchivo().length > 0) {
                // Si el usuario ya editó el Word, usamos su versión actual para no perder cambios
                in = new ByteArrayInputStream(oficio.getArchivo());
            } else {
                // Si es la primera vez, usamos la plantilla de resources
                in = new ClassPathResource("templates/oficio_toxicologia.docx").getInputStream();
            }

            IXDocReport report = XDocReportRegistry.getRegistry().loadReport(in, TemplateEngineKind.Velocity);
            IContext context = report.createContext();

            // 2. INYECTAR DATOS (Esto solo reemplazará los $placeholders que sigan existiendo)
            String fechaFormateada = formatearFechaLarga(oficio.getFecha());
            context.put("f_fecha", fechaFormateada);
            context.put("f_oficio", safeString(oficio.getNro_oficio()));
            context.put("f_grado", safeString(oficio.getGradoPNP()));
            context.put("f_responsablePNP", safeString(oficio.getNombresyapellidosPNP()));
            context.put("f_nro_informe_referencia", safeString(oficio.getNro_informe_referencia()));

            if (docBase != null) {
                context.put("d_nombre", safeString(docBase.getNombresyapellidos()));
                context.put("d_dni", safeString(docBase.getDni()));
                context.put("d_edad", safeString(docBase.getEdad()));
                context.put("d_muestra", safeString(docBase.getTipoMuestra()));
                context.put("d_informe", safeString(docBase.getNumeroInforme()));
            }

            // 3. GENERAR Y GUARDAR
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            report.process(context, out);

            oficio.setArchivo(out.toByteArray());
            repository.save(oficio);
            System.out.println("✅ Sincronización exitosa preservando cambios manuales.");

        } catch (Exception e) {
            throw new RuntimeException("Error en sincronización: " + e.getMessage());
        }
    }

    private String safeString(Object val) {
        return (val == null) ? " " : String.valueOf(val);
    }

    private String formatearFechaLarga(String fechaIso) {
        if (fechaIso == null || fechaIso.trim().isEmpty()) {
            return " ";
        }
        try {
            // Asumiendo que llega como "2026-01-26" desde el input date
            LocalDate fecha = LocalDate.parse(fechaIso);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d 'de' MMMM 'del' yyyy", new Locale("es", "ES"));
            return fecha.format(formatter);
        } catch (Exception e) {
            return fechaIso; // Si falla, devuelve el original para no romper el flujo
        }
    }
}

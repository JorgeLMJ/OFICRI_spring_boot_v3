package com.example.sistema_web.service;

import com.example.sistema_web.dto.OficioDosajeDTO;
import com.example.sistema_web.model.Documento;
import com.example.sistema_web.model.OficioDosaje;
import com.example.sistema_web.repository.DocumentoRepository;
import com.example.sistema_web.repository.OficioDosajeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class OficioDosajeServiceImpl implements OficioDosajeService {

    private final OficioDosajeRepository repository;
    private final DocumentoRepository documentoRepository;

    @Override
    public OficioDosajeDTO crear(OficioDosajeDTO dto) {
        Documento doc = documentoRepository.findById(dto.getDocumentoId())
                .orElseThrow(() -> new RuntimeException("Documento no encontrado"));

        OficioDosaje oficio = OficioDosaje.builder()
                .fecha(dto.getFecha())
                .nro_oficio(dto.getNro_oficio())
                .gradoPNP(dto.getGradoPNP())
                .nombresyapellidosPNP(dto.getNombresyapellidosPNP())
                .referencia(dto.getReferencia())
                .nro_informe(dto.getNro_informe())
                .documento(doc)
                .build();

        return mapToDTO(repository.save(oficio));
    }

    @Override
    public byte[] obtenerContenidoArchivo(Long id) {
        OficioDosaje oficio = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Oficio no encontrado con ID: " + id));
        Documento doc = oficio.getDocumento();

        if (doc != null && doc.getArchivo() != null && doc.getArchivo().length > 0) {
            return doc.getArchivo();
        }

        try {
            Resource resource = new ClassPathResource("templates/oficio_dosaje.docx");
            if (!resource.exists()) throw new RuntimeException("No existe la plantilla en la ruta definida");
            return resource.getInputStream().readAllBytes();
        } catch (Exception e) {
            throw new RuntimeException("Error al leer la plantilla base: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> getEditorConfig(Long id, String mode) {
        OficioDosaje oficio = repository.findById(id).orElseThrow();

        Map<String, Object> config = new HashMap<>();
        config.put("documentType", "word");

        Map<String, Object> document = new HashMap<>();
        document.put("fileType", "docx");
        document.put("key", "OFICIO_" + oficio.getId() + "_" + System.currentTimeMillis());
        document.put("title", "oficio_dosaje_" + (oficio.getNro_oficio() != null ? oficio.getNro_oficio() : oficio.getId()) + ".docx");

        // ⚠️ CAMBIO CRÍTICO: Usa tu IP real aquí para que OnlyOffice pueda conectar
        String ipServidor = "192.168.1.18";
        document.put("url", "http://" + ipServidor + ":8080/api/oficio-dosaje/" + id + "/download");

        config.put("document", document);

        Map<String, Object> editorConfig = new HashMap<>();
        editorConfig.put("mode", mode);

        // ⚠️ CAMBIO CRÍTICO: Esta es la URL donde OnlyOffice enviará el archivo para guardar
        editorConfig.put("callbackUrl", "http://" + ipServidor + ":8080/api/oficio-dosaje/" + id + "/callback");

        config.put("editorConfig", editorConfig);
        return config;
    }

    @Override
    @Transactional // ✅ CRÍTICO: Asegura que los cambios se confirmen (COMMIT) en la BD al finalizar
    public void actualizarDesdeOnlyOffice(Long id, String urlDescarga) {
        try {
            System.out.println("⬇️ Intentando descargar archivo desde: " + urlDescarga);

            URL url = new URL(urlDescarga);
            // Abrir conexión manualmente para añadir User-Agent y evitar el error 403
            java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");

            byte[] archivoBytes;
            try (InputStream in = connection.getInputStream()) {
                archivoBytes = in.readAllBytes();
            }

            // Buscamos el Oficio por ID
            OficioDosaje oficio = repository.findById(id)
                    .orElseThrow(() -> new RuntimeException("No existe el Oficio con ID: " + id));

            // 1. ✅ ASIGNACIÓN DIRECTA: Guardamos los bytes en el objeto OficioDosaje
            oficio.setArchivo(archivoBytes);

            // 2. SINCRONIZACIÓN: También guardamos en la entidad Documento vinculada
            Documento doc = oficio.getDocumento();
            if (doc != null) {
                doc.setArchivo(archivoBytes);
                documentoRepository.save(doc); // Guarda en la tabla 'documentos'
            }

            // 3. ✅ PERSISTENCIA FORZADA: Guardamos el oficio para actualizar la tabla 'oficio_dosaje'
            repository.save(oficio);

            System.out.println("✅ Archivo guardado y persistido. ID: " + id + " | Tamaño: " + archivoBytes.length + " bytes.");

        } catch (Exception e) {
            System.err.println("❌ ERROR CRÍTICO AL GUARDAR DESDE ONLYOFFICE:");
            e.printStackTrace();
            throw new RuntimeException("Fallo en descarga/guardado: " + e.getMessage());
        }
    }

    @Override
    public OficioDosajeDTO obtenerPorId(Long id) {
        return repository.findById(id).map(this::mapToDTO).orElseThrow();
    }

    @Override
    public List<OficioDosajeDTO> listar() {
        return repository.findAll().stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    @Override
    public OficioDosajeDTO actualizar(Long id, OficioDosajeDTO dto) {
        OficioDosaje oficio = repository.findById(id).orElseThrow();
        oficio.setFecha(dto.getFecha());
        oficio.setNro_oficio(dto.getNro_oficio());
        oficio.setGradoPNP(dto.getGradoPNP());
        oficio.setNombresyapellidosPNP(dto.getNombresyapellidosPNP());
        oficio.setReferencia(dto.getReferencia());
        oficio.setNro_informe(dto.getNro_informe());
        return mapToDTO(repository.save(oficio));
    }

    @Override
    public void eliminar(Long id) {
        repository.deleteById(id);
    }

    private OficioDosajeDTO mapToDTO(OficioDosaje oficio) {
        OficioDosajeDTO dto = new OficioDosajeDTO();
        dto.setId(oficio.getId());
        dto.setFecha(oficio.getFecha());
        dto.setNro_oficio(oficio.getNro_oficio());
        dto.setGradoPNP(oficio.getGradoPNP());
        dto.setNombresyapellidosPNP(oficio.getNombresyapellidosPNP());
        dto.setReferencia(oficio.getReferencia());
        dto.setNro_informe(oficio.getNro_informe());
        if (oficio.getDocumento() != null) {
            dto.setDocumentoId(oficio.getDocumento().getId());
        }
        return dto;
    }
}
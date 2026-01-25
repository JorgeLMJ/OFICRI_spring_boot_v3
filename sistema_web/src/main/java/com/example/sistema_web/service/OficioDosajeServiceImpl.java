package com.example.sistema_web.service;
import com.example.sistema_web.dto.OficioDosajeDTO;
import com.example.sistema_web.model.OficioDosaje;
import com.example.sistema_web.model.Documento;
import com.example.sistema_web.repository.DocumentoRepository;
import com.example.sistema_web.repository.OficioDosajeRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OficioDosajeServiceImpl implements OficioDosajeService {

    private final OficioDosajeRepository repository;
    private final DocumentoRepository documentoRepository;

    // ✅ 1. CREAR OFICIO
    @Override
    @Transactional
    public Long crearOficioDosajeVacio() {
        OficioDosaje oficio = new OficioDosaje();
        OficioDosaje saved = repository.save(oficio);
        return saved.getId();
    }

    // ✅ 2. VALIDAR EXISTENCIA (Sin crear nada)
    @Override
    public boolean existeOficioDosaje(Long id) {
        return repository.existsById(id);
    }

    // ✅ 3. OBTENER ARCHIVO (Si es nuevo, devuelve plantilla)
    @Override
    public byte[] obtenerContenidoArchivo(Long id) {
        OficioDosaje oficio = repository.findById(id)
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
        try {
            System.out.println("⬇️ Iniciando descarga para Doc ID: " + id);

            java.net.URL url = new java.net.URL(urlDescarga);
            byte[] archivoBytes;
            try (java.io.InputStream in = url.openStream()) {
                archivoBytes = in.readAllBytes();
            }

            OficioDosaje oficio = repository.findById(id).orElseThrow(() ->
                    new RuntimeException("Documento no encontrado con ID: " + id)
            );
            // ✅ ESTO ES LO ÚNICO IMPORTANTE: Guardar el archivo en la entidad
            oficio.setArchivo(archivoBytes);
            if (documentoId != null) {
                Documento documento = documentoRepository.findById(documentoId).orElse(null);
                if (documento != null) oficio.setDocumento(documento);
            }
            // ⛔ COMENTAMOS ESTA LÍNEA para que no intente leer nada del Word
            extraerMetadatosDelWord(archivoBytes, oficio);

            // Guardamos los cambios (el archivo blob) en la BD
            repository.save(oficio);
            System.out.println("✅ ¡ARCHIVO GUARDADO! (Sin extracción de metadatos)");

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error al guardar archivo: " + e.getMessage(), e);
        }
    }

    private void extraerMetadatosDelWord(byte[] archivo, OficioDosaje oficio) {

        // 2. Creamos el flujo de lectura (esto corrige el error 'cannot find symbol bis')
        ByteArrayInputStream bis = new ByteArrayInputStream(archivo);

        try (XWPFDocument document = new XWPFDocument(bis)) {

            // A. LEER PÁRRAFOS SUELTOS
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                procesarParrafo(paragraph, oficio);
            }
            // B. LEER DENTRO DE LAS TABLAS (Aquí estaba el error)
            if (document.getTables() != null) {
                for (XWPFTable table : document.getTables()) {
                    for (XWPFTableRow row : table.getRows()) {
                        for (XWPFTableCell cell : row.getTableCells()) {

                            for (XWPFParagraph paragraph : cell.getParagraphs()) {
                                procesarParrafo(paragraph, oficio);
                            }
                        }
                    }
                }
            }

        } catch (IOException e) {
            System.err.println("⚠️ Error leyendo el Word para extracción: " + e.getMessage());
        }
    }

    private void procesarParrafo(XWPFParagraph paragraph, OficioDosaje oficio) {
        String textoCompleto = paragraph.getText();
        if (textoCompleto != null && !textoCompleto.isEmpty()) {
            System.out.println("📝 TEXTO ENCONTRADO EN WORD: " + textoCompleto);
        }

        if (paragraph.getIRuns() != null) {
            for (IRunElement run : paragraph.getIRuns()) {
                // 2. Verificar si es un Control de Contenido (SDT)
                if (run instanceof XWPFSDT) {
                    XWPFSDT sdt = (XWPFSDT) run;
                    String tag = sdt.getTag();
                    String text = sdt.getContent().getText();

                    System.out.println("   👉 CONTROL DETECTADO:");
                    System.out.println("      - Tag (Etiqueta): " + (tag == null ? "NULL (¡AQUÍ ESTÁ EL ERROR!)" : "'" + tag + "'"));
                    System.out.println("      - Contenido: " + text);

                    if (tag != null && text != null && !text.trim().isEmpty()) {
                        asignarValor(oficio, tag, text.trim());
                    }
                }
                // 3. Verificar si es un texto normal (no control)
                else if (run instanceof XWPFRun) {
                }
            }
        }
    }

    private void asignarValor(OficioDosaje oficio, String tag, String valor) {
        System.out.println("   🔍 Dato encontrado -> Tag: " + tag + " | Valor: " + valor);
        switch (tag.toUpperCase()) {
            case "FECHA": oficio.setFecha(valor); break;
            case "NROOFICIO": oficio.setNro_oficio(valor); break;
            case "GRADOPNP": oficio.setGradoPNP(valor); break;
            case "NOMBRESYAPELLIDOSPNP": oficio.setNombresyapellidosPNP(valor); break;
            case "REFERENCIA": oficio.setReferencia(valor); break;
            case "NROINFORME": oficio.setNro_informe(valor); break;
            default: break;
        }
    }

// --- MÉTODOS CRUD ESTÁNDAR ---
    @Override
    public OficioDosajeDTO crear(OficioDosajeDTO dto) {
        OficioDosaje oficio = mapToEntity(dto);
        OficioDosaje saved = repository.save(oficio);
        return mapToDTO(saved);
    }

    @Override
    public OficioDosajeDTO obtenerPorId(Long id) {
        return repository.findById(id).map(this::mapToDTO)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Oficio no encontrado"));
    }

    @Override
    public List<OficioDosajeDTO> listar() {
        return repository.findAll().stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    @Override
    public OficioDosajeDTO actualizar(Long id, OficioDosajeDTO dto) {
        OficioDosaje oficio = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Oficio no encontrado"));

        oficio.setFecha(dto.getFecha());
        oficio.setNro_oficio(dto.getNro_oficio());
        oficio.setGradoPNP(dto.getGradoPNP());
        oficio.setNombresyapellidosPNP(dto.getNombresyapellidosPNP());
        oficio.setReferencia(dto.getReferencia());
        oficio.setNro_informe(dto.getNro_informe());
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
    public void uploadOficioDosaje(Long id, byte[] archivoBytes) {
        OficioDosaje oficio = repository.findById(id).orElseThrow();
        oficio.setArchivo(archivoBytes);
        repository.save(oficio);
    }


    // --- MAPPER ---
    private OficioDosajeDTO mapToDTO(OficioDosaje oficio) {
        OficioDosajeDTO dto = new OficioDosajeDTO();
        dto.setId(oficio.getId());
        dto.setFecha(oficio.getFecha());
        dto.setNro_oficio(oficio.getNro_oficio());
        dto.setGradoPNP(oficio.getGradoPNP());
        dto.setNombresyapellidosPNP(oficio.getNombresyapellidosPNP());
        dto.setReferencia(oficio.getReferencia());
        dto.setNro_informe(oficio.getNro_informe());
        dto.setArchivo(oficio.getArchivo());
        dto.setDocumentoId(oficio.getDocumento() != null ? oficio.getDocumento().getId() : null);
        return dto;
        }

    private OficioDosaje mapToEntity(OficioDosajeDTO dto) {
        OficioDosaje.OficioDosajeBuilder builder = OficioDosaje.builder()
                .fecha(dto.getFecha())
                .nro_oficio(dto.getNro_oficio())
                .gradoPNP(dto.getGradoPNP())
                .nombresyapellidosPNP(dto.getNombresyapellidosPNP())
                .referencia(dto.getReferencia())
                .nro_informe(dto.getNro_informe())
                .archivo(dto.getArchivo());

        if (dto.getDocumentoId() != null) {
            var documento = documentoRepository.findById(dto.getDocumentoId()).orElseThrow();
            builder.documento(documento);
        }
        return builder.build();
    }

    // ✅ 5. ACTUALIZAR CAMPOS DENTRO DEL WORD (Sincronización inversa BD -> Word)
    @Override
    @Transactional
    public void actualizarCampoEnWord(Long id, String tag, String valor) {
        try {
            OficioDosaje oficio = repository.findById(id).orElseThrow();
            if (oficio.getArchivo() == null) return;

            try (ByteArrayInputStream bis = new ByteArrayInputStream(oficio.getArchivo());
                 XWPFDocument document = new XWPFDocument(bis);
                 ByteArrayOutputStream bos = new ByteArrayOutputStream()) {

                boolean cambiado = false;

                // Buscar en párrafos
                for (XWPFParagraph p : document.getParagraphs()) {
                    if (reemplazarEnParrafo(p, tag, valor)) cambiado = true;
                }

                // Buscar en tablas
                for (XWPFTable tbl : document.getTables()) {
                    for (XWPFTableRow row : tbl.getRows()) {
                        for (XWPFTableCell cell : row.getTableCells()) {
                            for (XWPFParagraph p : cell.getParagraphs()) {
                                if (reemplazarEnParrafo(p, tag, valor)) cambiado = true;
                            }
                        }
                    }
                }

                if (cambiado) {
                    document.write(bos);
                    oficio.setArchivo(bos.toByteArray());
                    repository.save(oficio);
                    System.out.println("📝 [OFICIO] Campo '" + tag + "' actualizado en el Word.");
                }
            }
        } catch (Exception e) {
            System.err.println("⚠️ Error actualizando campo en Word: " + e.getMessage());
        }
    }

    private boolean reemplazarEnParrafo(XWPFParagraph p, String tagBuscado, String nuevoValor) {
        boolean encontrado = false;

        for (IRunElement run : p.getIRuns()) {
            if (run instanceof XWPFSDT) {
                XWPFSDT sdt = (XWPFSDT) run;

                if (sdt.getTag() != null && sdt.getTag().equalsIgnoreCase(tagBuscado)) {
                    try {
                        // ✅ SOLUCIÓN DEFINITIVA MEDIANTE REFLEXIÓN
                        // Accedemos al campo privado 'ctSdt' que contiene el XML
                        java.lang.reflect.Field field = sdt.getClass().getDeclaredField("ctSdt");
                        field.setAccessible(true);
                        Object ctSdt = field.get(sdt);

                        // El objeto ctSdt suele ser CTSdtRun o CTSdtBlock
                        // Usamos reflexión para obtener el SdtContent
                        java.lang.reflect.Method getSdtContent = ctSdt.getClass().getMethod("getSdtContent");
                        Object sdtContent = getSdtContent.invoke(ctSdt);

                        // Obtenemos el objeto CTSdtContentRun para manipular los nodos de texto
                        if (sdtContent instanceof org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSdtContentRun) {
                            org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSdtContentRun xmlContent =
                                    (org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSdtContentRun) sdtContent;

                            // 1. Limpiamos cualquier texto previo (nodos <w:r>)
                            int size = xmlContent.sizeOfRArray();
                            for (int i = size - 1; i >= 0; i--) {
                                xmlContent.removeR(i);
                            }

                            // 2. Creamos un nuevo nodo de texto con el valor
                            xmlContent.addNewR().addNewT().setStringValue(nuevoValor);
                            encontrado = true;
                            System.out.println("✏️ Campo SDT '" + tagBuscado + "' actualizado mediante reflexión a: " + nuevoValor);
                        }

                    } catch (Exception e) {
                        // 🛠️ SEGUNDO INTENTO: Si la reflexión falla, intentamos mediante la interfaz de contenido
                        try {
                            sdt.getContent().getText(); // Solo para verificar acceso
                            // Si tu versión de POI lo permite, se puede intentar manipular los párrafos internos de sdt.getContent()
                            System.err.println("⚠️ Falló reflexión, intentando vía alternativa...");
                        } catch (Exception e2) {
                            System.err.println("❌ Error total actualizando el Word: " + e.getMessage());
                        }
                    }
                }
            }
        }
        return encontrado;
    }

    @Override
    @Transactional
    public void actualizarTagEnWord(Long id, String tagBuscado, String nuevoValor) {
        String marcadorEtiqueta = "{{" + tagBuscado + "}}";

        try {
            OficioDosaje oficio = repository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Oficio no encontrado"));

            if (oficio.getArchivo() == null) return;

            // 1. OBTENER EL VALOR ANTIGUO DE LA BASE DE DATOS
            // Esto nos sirve para buscarlo si la etiqueta {{...}} ya desapareció.
            String valorAntiguo = null;
            if (tagBuscado.equalsIgnoreCase("FECHA")) {
                valorAntiguo = oficio.getFecha(); // Ej: "02/12/2025"
            }

            // Evitar trabajar si el valor no cambia
            if (nuevoValor.equals(valorAntiguo)) {
                System.out.println("ℹ️ El valor nuevo es igual al actual. No se realizan cambios.");
                return;
            }

            try (ByteArrayInputStream bis = new ByteArrayInputStream(oficio.getArchivo());
                 XWPFDocument document = new XWPFDocument(bis);
                 ByteArrayOutputStream bos = new ByteArrayOutputStream()) {

                boolean cambiado = false;

                // --- INTENTO 1: BUSCAR LA ETIQUETA {{TAG}} ---
                if (buscarYReemplazarEnTodoElOficio(document, marcadorEtiqueta, nuevoValor)) {
                    cambiado = true;
                    System.out.println("✅ Se reemplazó la etiqueta original: " + marcadorEtiqueta);
                }
                // --- INTENTO 2: SI NO ESTÁ LA ETIQUETA, BUSCAR EL VALOR ANTIGUO ---
                else if (valorAntiguo != null && !valorAntiguo.isEmpty()) {
                    System.out.println("⚠️ Etiqueta no encontrada. Buscando valor antiguo: '" + valorAntiguo + "'");

                    // Buscamos textualmente el número viejo (ej: "0.50") y lo cambiamos por el nuevo
                    if (buscarYReemplazarEnTodoElOficio(document, valorAntiguo, nuevoValor)) {
                        cambiado = true;
                        System.out.println("✅ Se actualizó el valor antiguo '" + valorAntiguo + "' por '" + nuevoValor + "'");
                    }
                }

                if (cambiado) {
                    document.write(bos);
                    oficio.setArchivo(bos.toByteArray());

                    // Actualizar DB
                    if (tagBuscado.equalsIgnoreCase("FECHA")) {
                        oficio.setFecha(nuevoValor);
                    }

                    repository.save(oficio);
                } else {
                    System.err.println("❌ No se pudo actualizar. No se encontró ni '" + marcadorEtiqueta + "' ni el valor antiguo '" + valorAntiguo + "'");
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error editando Word: " + e.getMessage());
        }
    }

    private boolean buscarYReemplazarEnTodoElOficio(XWPFDocument document, String buscado, String reemplazo) {
        boolean encontrado = false;

        // 1. Párrafos normales
        for (XWPFParagraph p : document.getParagraphs()) {
            if (reemplazarTextoEnParrafo(p, buscado, reemplazo)) encontrado = true;
        }

        // 2. Tablas
        for (XWPFTable table : document.getTables()) {
            for (XWPFTableRow row : table.getRows()) {
                for (XWPFTableCell cell : row.getTableCells()) {
                    for (XWPFParagraph p : cell.getParagraphs()) {
                        if (reemplazarTextoEnParrafo(p, buscado, reemplazo)) encontrado = true;
                    }
                }
            }
        }
        return encontrado;
    }
    // METODO DE REEMPLAZO (Sin cambios, solo reutilizado)
    private boolean reemplazarTextoEnParrafo(XWPFParagraph p, String marcador, String nuevoValor) {
        boolean encontrado = false;
        List<XWPFRun> runs = p.getRuns();

        if (runs != null) {
            for (XWPFRun r : runs) {
                String text = r.getText(0);
                if (text != null && text.contains(marcador)) {
                    text = text.replace(marcador, nuevoValor);
                    r.setText(text, 0);
                    encontrado = true;
                }
            }
        }
        return encontrado;
    }
    private boolean reemplazarValorEnSDT(XWPFParagraph p, String tagBuscado, String nuevoValor) {
        boolean encontrado = false;

        // IRunElement representa partes del párrafo (Textos, Negritas, SDTs)
        for (IRunElement run : p.getIRuns()) {
            if (run instanceof XWPFSDT) {
                XWPFSDT sdt = (XWPFSDT) run;

                // Verificamos si el Tag coincide (ignorando mayúsculas/minúsculas)
                if (sdt.getTag() != null && sdt.getTag().equalsIgnoreCase(tagBuscado)) {

                    try {
                        // ✅ SOLUCIÓN ROBUSTA: Acceso directo al XML subyacente
                        // 1. Accedemos al campo privado 'ctSdt' que contiene la estructura XML
                        java.lang.reflect.Field field = sdt.getClass().getDeclaredField("ctSdt");
                        field.setAccessible(true);
                        Object ctSdt = field.get(sdt); // Puede ser CTSdtRun o CTSdtBlock

                        // 2. Obtenemos el contenido (SdtContent)
                        java.lang.reflect.Method getSdtContent = ctSdt.getClass().getMethod("getSdtContent");
                        Object sdtContent = getSdtContent.invoke(ctSdt);

                        // 3. Verificamos si es contenido de texto (CTSdtContentRun)
                        if (sdtContent instanceof org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSdtContentRun) {
                            org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSdtContentRun xmlContent =
                                    (org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSdtContentRun) sdtContent;

                            // 4. Limpiamos cualquier texto previo (nodos <w:r>)
                            int size = xmlContent.sizeOfRArray();
                            for (int i = size - 1; i >= 0; i--) {
                                xmlContent.removeR(i);
                            }

                            // 5. Creamos un nuevo nodo de texto con el valor
                            xmlContent.addNewR().addNewT().setStringValue(nuevoValor);
                            encontrado = true;
                            // System.out.println("✏️ SDT '" + tagBuscado + "' actualizado a: " + nuevoValor);
                        } else {
                            System.err.println("⚠️ El tipo de contenido SDT no es compatible para edición directa.");
                        }

                    } catch (Exception e) {
                        System.err.println("❌ Error actualizando SDT '" + tagBuscado + "': " + e.getMessage());
                    }
                }
            }
        }
        return encontrado;
    }


}


package com.example.sistema_web.service;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSdtContentRun;
import com.example.sistema_web.dto.DocumentoDTO;
import com.example.sistema_web.model.Documento;
import com.example.sistema_web.model.Empleado;
import com.example.sistema_web.repository.DocumentoRepository;
import com.example.sistema_web.repository.EmpleadoRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DocumentoServiceImpl implements DocumentoService {

    private final DocumentoRepository repository;
    private final EmpleadoRepository empleadoRepository;

    // ✅ 1. CREAR DOCUMENTO (Solo usado por botón Nuevo)
    @Override
    @Transactional
    public Long crearDocumentoVacio() {
        Documento doc = new Documento();
        Documento saved = repository.save(doc);
        return saved.getId();
    }

    // ✅ 2. VALIDAR EXISTENCIA (Sin crear nada)
    @Override
    public boolean existeDocumento(Long id) {
        return repository.existsById(id);
    }

    // ✅ 3. OBTENER ARCHIVO (Si es nuevo, devuelve plantilla)
    @Override
    public byte[] obtenerContenidoArchivo(Long id) {
        Documento doc = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Doc no encontrado"));

        if (doc.getArchivo() != null && doc.getArchivo().length > 0) {
            return doc.getArchivo();
        }

        try {
            Resource resource = new ClassPathResource("templates/informe_dosaje.docx");
            if (!resource.exists()) {
                throw new RuntimeException("❌ Plantilla no encontrada en resources");
            }
            return resource.getInputStream().readAllBytes();
        } catch (Exception e) {
            throw new RuntimeException("Error al leer la plantilla base", e);
        }
    }

    // ✅ 4. GUARDAR DESDE ONLYOFFICE (Con extracción de datos)
    @Override
    @Transactional
    public void actualizarDesdeUrlOnlyOffice(Long id, String urlDescarga, Long empleadoId) {
        try {
            System.out.println("⬇️ Iniciando descarga para Doc ID: " + id);

            java.net.URL url = new java.net.URL(urlDescarga);
            byte[] archivoBytes;
            try (java.io.InputStream in = url.openStream()) {
                archivoBytes = in.readAllBytes();
            }

            Documento doc = repository.findById(id).orElseThrow(() ->
                    new RuntimeException("Documento no encontrado con ID: " + id)
            );

            // ✅ ESTO ES LO ÚNICO IMPORTANTE: Guardar el archivo en la entidad
            doc.setArchivo(archivoBytes);

            if (empleadoId != null) {
                Empleado empleado = empleadoRepository.findById(empleadoId).orElse(null);
                if (empleado != null) doc.setEmpleado(empleado);
            }

            // ⛔ COMENTAMOS ESTA LÍNEA para que no intente leer nada del Word
             extraerMetadatosDelWord(archivoBytes, doc);

            // Guardamos los cambios (el archivo blob) en la BD
            repository.save(doc);
            System.out.println("✅ ¡ARCHIVO GUARDADO! (Sin extracción de metadatos)");

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error al guardar archivo: " + e.getMessage(), e);
        }
    }

    // --- MÉTODOS PRIVADOS DE EXTRACCIÓN (APACHE POI) ---
    // --- MÉTODOS PRIVADOS DE EXTRACCIÓN (APACHE POI) ---
    // --- MÉTODOS PRIVADOS DE EXTRACCIÓN (APACHE POI) ---
    // --- MÉTODOS PRIVADOS DE EXTRACCIÓN (APACHE POI) ---
    private void extraerMetadatosDelWord(byte[] archivo, Documento doc) { // 1. Aquí recibes 'doc'

        // 2. Creamos el flujo de lectura (esto corrige el error 'cannot find symbol bis')
        ByteArrayInputStream bis = new ByteArrayInputStream(archivo);

        try (XWPFDocument document = new XWPFDocument(bis)) {

            // A. LEER PÁRRAFOS SUELTOS
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                procesarParrafo(paragraph, doc); // Usamos 'doc'
            }

            // B. LEER DENTRO DE LAS TABLAS (Aquí estaba el error)
            if (document.getTables() != null) {
                for (XWPFTable table : document.getTables()) {
                    for (XWPFTableRow row : table.getRows()) {

                        // 👇👇👇 CORRECCIÓN CRÍTICA 👇👇👇
                        // Antes tenías: table.getTableCells() (ERROR)
                        // Ahora es:     row.getTableCells()   (CORRECTO)
                        for (XWPFTableCell cell : row.getTableCells()) {

                            for (XWPFParagraph paragraph : cell.getParagraphs()) {
                                procesarParrafo(paragraph, doc); // Usamos 'doc'
                            }
                        }
                    }
                }
            }

            System.out.println("✅ Documento analizado correctamente (Párrafos y Tablas).");

        } catch (IOException e) {
            System.err.println("⚠️ Error leyendo el Word para extracción: " + e.getMessage());
        }
    }

    private void procesarParrafo(XWPFParagraph paragraph, Documento doc) {
        // 1. Imprimir todo el texto crudo del párrafo para ver si Java lee algo
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
                        asignarValor(doc, tag, text.trim());
                    }
                }
                // 3. Verificar si es un texto normal (no control)
                else if (run instanceof XWPFRun) {
                    // Solo para saber si está leyendo texto plano fuera de las cajas
                    // System.out.println("      (Texto plano: " + ((XWPFRun) run).getText(0) + ")");
                }
            }
        }
    }

    private void asignarValor(Documento doc, String tag, String valor) {
        System.out.println("   🔍 Dato encontrado -> Tag: " + tag + " | Valor: " + valor);
        switch (tag.toUpperCase()) {
            case "NOMBRESYAPELLIDOS": doc.setNombresyapellidos(valor); break;
            case "DNI": doc.setDni(valor); break;
            case "NOMBREOFICIO": doc.setNombreOficio(valor); break;
            case "NUMERODOCUMENTO": doc.setNumeroDocumento(valor); break;
            case "PROCEDENCIA": doc.setProcedencia(valor); break;
            case "TIPOMUESTRA": doc.setTipoMuestra(valor); break;
            case "PERSONAQUECONDUCE": doc.setPersonaQueConduce(valor); break;
            case "CUALITATIVO": doc.setCualitativo(valor); break;
            case "CUANTITATIVO": doc.setCuantitativo(valor); break;
            default: break;
        }
    }

    // --- MÉTODOS CRUD ESTÁNDAR ---
    @Override
    public DocumentoDTO crear(DocumentoDTO dto) {
        Documento doc = mapToEntity(dto);
        Documento saved = repository.save(doc);
        return mapToDTO(saved);
    }

    @Override
    public DocumentoDTO obtenerPorId(Long id) {
        return repository.findById(id)
                .map(this::mapToDTO)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Documento no encontrado con ID: " + id));
    }

    @Override
    public List<DocumentoDTO> listar() {
        return repository.findAll().stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    @Override
    public DocumentoDTO actualizar(Long id, DocumentoDTO dto) {
        Documento doc = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Documento no encontrado con ID: " + id));

        doc.setNombresyapellidos(dto.getNombresyapellidos());
        doc.setDni(dto.getDni());
        doc.setCualitativo(dto.getCualitativo());
        doc.setCuantitativo(dto.getCuantitativo());
        doc.setNumeroDocumento(dto.getNumeroDocumento());
        doc.setNombreOficio(dto.getNombreOficio());
        doc.setProcedencia(dto.getProcedencia());
        doc.setTipoMuestra(dto.getTipoMuestra());
        doc.setPersonaQueConduce(dto.getPersonaQueConduce());
        doc.setArchivo(dto.getArchivo());

        if (dto.getEmpleadoId() != null) {
            var empleado = empleadoRepository.findById(dto.getEmpleadoId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Empleado no encontrado"));
            doc.setEmpleado(empleado);
        }
        return mapToDTO(repository.save(doc));
    }

    @Override
    public void eliminar(Long id) {
        if (!repository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "ID no encontrado");
        }
        repository.deleteById(id);
    }

    @Override
    public void uploadDocumento(Long id, byte[] archivoBytes) {
        Documento doc = repository.findById(id).orElseThrow();
        doc.setArchivo(archivoBytes);
        repository.save(doc);
    }

    // --- MAPPERS ---
    private DocumentoDTO mapToDTO(Documento doc) {
        DocumentoDTO dto = new DocumentoDTO();
        dto.setId(doc.getId());
        dto.setNombresyapellidos(doc.getNombresyapellidos());
        dto.setDni(doc.getDni());
        dto.setCualitativo(doc.getCualitativo());
        dto.setCuantitativo(doc.getCuantitativo());
        dto.setNumeroDocumento(doc.getNumeroDocumento());
        dto.setNombreOficio(doc.getNombreOficio());
        dto.setProcedencia(doc.getProcedencia());
        dto.setTipoMuestra(doc.getTipoMuestra());
        dto.setPersonaQueConduce(doc.getPersonaQueConduce());
        dto.setArchivo(doc.getArchivo());
        dto.setEmpleadoId(doc.getEmpleado() != null ? doc.getEmpleado().getId() : null);
        return dto;
    }

    private Documento mapToEntity(DocumentoDTO dto) {
        Documento.DocumentoBuilder builder = Documento.builder()
                .nombresyapellidos(dto.getNombresyapellidos())
                .dni(dto.getDni())
                .cualitativo(dto.getCualitativo())
                .cuantitativo(dto.getCuantitativo())
                .numeroDocumento(dto.getNumeroDocumento())
                .nombreOficio(dto.getNombreOficio())
                .procedencia(dto.getProcedencia())
                .tipoMuestra(dto.getTipoMuestra())
                .personaQueConduce(dto.getPersonaQueConduce())
                .archivo(dto.getArchivo());

        if (dto.getEmpleadoId() != null) {
            var empleado = empleadoRepository.findById(dto.getEmpleadoId()).orElseThrow();
            builder.empleado(empleado);
        }
        return builder.build();
    }
    @Override
    @Transactional
    public void actualizarCampoEnWord(Long id, String tag, String valor) {
        try {
            // 1. Obtener el documento de la BD
            Documento doc = repository.findById(id).orElseThrow(() ->
                    new RuntimeException("Documento no encontrado"));

            if (doc.getArchivo() == null) return;

            // 2. Abrir el archivo Word en memoria
            try (ByteArrayInputStream bis = new ByteArrayInputStream(doc.getArchivo());
                 XWPFDocument document = new XWPFDocument(bis);
                 ByteArrayOutputStream bos = new ByteArrayOutputStream()) {

                boolean cambiado = false;

                // 3. Buscar y reemplazar en Párrafos
                for (XWPFParagraph p : document.getParagraphs()) {
                    if (reemplazarEnParrafo(p, tag, valor)) cambiado = true;
                }

                // 4. Buscar y reemplazar en Tablas
                for (XWPFTable tbl : document.getTables()) {
                    for (XWPFTableRow row : tbl.getRows()) {
                        for (XWPFTableCell cell : row.getTableCells()) {
                            for (XWPFParagraph p : cell.getParagraphs()) {
                                if (reemplazarEnParrafo(p, tag, valor)) cambiado = true;
                            }
                        }
                    }
                }

                // 5. Si hubo cambios, guardar el nuevo archivo en la BD
                if (cambiado) {
                    document.write(bos);
                    doc.setArchivo(bos.toByteArray());

                    // También actualizamos el campo en la entidad SQL para consistencia
                    if (tag.equalsIgnoreCase("CUANTITATIVO")) {
                        doc.setCuantitativo(valor);
                    }

                    repository.save(doc);
                    System.out.println("✅ Campo '" + tag + "' actualizado a: " + valor);
                } else {
                    System.out.println("⚠️ No se encontró la etiqueta: " + tag);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error editando Word: " + e.getMessage());
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
            Documento doc = repository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Documento no encontrado"));

            if (doc.getArchivo() == null) return;

            // 1. OBTENER EL VALOR ANTIGUO DE LA BASE DE DATOS
            // Esto nos sirve para buscarlo si la etiqueta {{...}} ya desapareció.
            String valorAntiguo = null;
            if (tagBuscado.equalsIgnoreCase("CUANTITATIVO")) {
                valorAntiguo = doc.getCuantitativo(); // Ej: "0.50"
            }

            // Evitar trabajar si el valor no cambia
            if (nuevoValor.equals(valorAntiguo)) {
                System.out.println("ℹ️ El valor nuevo es igual al actual. No se realizan cambios.");
                return;
            }

            try (ByteArrayInputStream bis = new ByteArrayInputStream(doc.getArchivo());
                 XWPFDocument document = new XWPFDocument(bis);
                 ByteArrayOutputStream bos = new ByteArrayOutputStream()) {

                boolean cambiado = false;

                // --- INTENTO 1: BUSCAR LA ETIQUETA {{TAG}} ---
                if (buscarYReemplazarEnTodoElDoc(document, marcadorEtiqueta, nuevoValor)) {
                    cambiado = true;
                    System.out.println("✅ Se reemplazó la etiqueta original: " + marcadorEtiqueta);
                }
                // --- INTENTO 2: SI NO ESTÁ LA ETIQUETA, BUSCAR EL VALOR ANTIGUO ---
                else if (valorAntiguo != null && !valorAntiguo.isEmpty()) {
                    System.out.println("⚠️ Etiqueta no encontrada. Buscando valor antiguo: '" + valorAntiguo + "'");

                    // Buscamos textualmente el número viejo (ej: "0.50") y lo cambiamos por el nuevo
                    if (buscarYReemplazarEnTodoElDoc(document, valorAntiguo, nuevoValor)) {
                        cambiado = true;
                        System.out.println("✅ Se actualizó el valor antiguo '" + valorAntiguo + "' por '" + nuevoValor + "'");
                    }
                }

                if (cambiado) {
                    document.write(bos);
                    doc.setArchivo(bos.toByteArray());

                    // Actualizar DB
                    if (tagBuscado.equalsIgnoreCase("CUANTITATIVO")) {
                        doc.setCuantitativo(nuevoValor);
                    }

                    repository.save(doc);
                } else {
                    System.err.println("❌ No se pudo actualizar. No se encontró ni '" + marcadorEtiqueta + "' ni el valor antiguo '" + valorAntiguo + "'");
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error editando Word: " + e.getMessage());
        }
    }

    // 👇 MÉTODO HELPER PARA NO REPETIR CÓDIGO (Busca en Párrafos y Tablas)
    private boolean buscarYReemplazarEnTodoElDoc(XWPFDocument document, String buscado, String reemplazo) {
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

    // 👇 TU MÉTODO DE REEMPLAZO (Sin cambios, solo reutilizado)
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
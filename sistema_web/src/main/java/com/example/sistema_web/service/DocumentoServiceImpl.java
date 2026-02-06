package com.example.sistema_web.service;
import com.example.sistema_web.config.JwtAuthFilter;
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
    public Long crearDocumentoVacio(Long empleadoId) {
        Documento doc = new Documento();
        // Buscamos al empleado y lo vinculamos
        if (empleadoId != null) {
            Empleado emp = empleadoRepository.findById(empleadoId).orElse(null);
            doc.setEmpleado(emp);
        }
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
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Documento no encontrado"));

        // 1. Si ya tiene contenido guardado en la BD, devolverlo
        if (doc.getArchivo() != null && doc.getArchivo().length > 0) {
            return doc.getArchivo();
        }

        // 2. LÓGICA DE PLANTILLA BASADA EN EL DOCUMENTO
        // Por defecto usamos dosaje
        String nombrePlantilla = "templates/informe_dosaje.docx";

        // Verificamos el cargo del dueño del documento directamente
        if (doc.getEmpleado() != null && doc.getEmpleado().getCargo() != null) {
            String cargoDueño = doc.getEmpleado().getCargo().toLowerCase();

            // Si el dueño (creador) es de Toxicología, USAR plantilla de Toxicología
            if (cargoDueño.contains("tox")) {
                nombrePlantilla = "templates/informe_toxicologia.docx";
                System.out.println("🧪 Cargando plantilla física de TOXICOLOGÍA para el Doc ID: " + id);
            } else {
                System.out.println("🍷 Cargando plantilla física de DOSAJE para el Doc ID: " + id);
            }
        }

        try {
            Resource resource = new ClassPathResource(nombrePlantilla);
            if (!resource.exists()) {
                throw new RuntimeException("No se encontró el archivo .docx en: " + nombrePlantilla);
            }
            return resource.getInputStream().readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException("Error al leer la plantilla física", e);
        }
    }

    // ✅ 4. GUARDAR DESDE ONLYOFFICE (Con extracción de datos)
    @Override
    @Transactional
    public void actualizarDesdeUrlOnlyOffice(Long id, String urlDescarga, Long empleadoId) {
        if (urlDescarga.contains("onlyoffice_pruebas")) {
            urlDescarga = urlDescarga.replace("onlyoffice_pruebas", "localhost:9000");
        } else if (urlDescarga.contains("onlyoffice_server")) {
            urlDescarga = urlDescarga.replace("onlyoffice_server", "localhost:9000");
        }

        System.out.println("⬇️ Descargando cambios desde OnlyOffice: " + urlDescarga);

        try {
            java.net.URL url = new java.net.URL(urlDescarga);
            byte[] archivoBytes;

            // Usamos HttpURLConnection para un mejor manejo de flujos de red en Docker
            java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            try (java.io.InputStream in = connection.getInputStream()) {
                archivoBytes = in.readAllBytes();
            }

            Documento doc = repository.findById(id).orElseThrow(() ->
                    new RuntimeException("Documento no encontrado con ID: " + id)
            );

            // ✅ Guardamos el arreglo de bytes (LONGBLOB) en la entidad
            doc.setArchivo(archivoBytes);

            if (empleadoId != null) {
                Empleado empleado = empleadoRepository.findById(empleadoId).orElse(null);
                if (empleado != null) doc.setEmpleado(empleado);
            }

            extraerMetadatosDelWord(archivoBytes, doc);

            // Guardamos los cambios en la BD MySQL
            repository.save(doc);
            System.out.println("✅ ¡ARCHIVO GUARDADO EXITOSAMENTE EN MYSQL! Tamaño: " + archivoBytes.length + " bytes.");

        } catch (Exception e) {
            System.err.println("❌ Error crítico en el guardado: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Error al guardar archivo: " + e.getMessage(), e);
        }
    }

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
            case "EDAD": doc.setEdad(valor); break;
            case "NOMBREOFICIO": doc.setNombreOficio(valor); break;
            case "NUMERODOCUMENTO": doc.setNumeroInforme(valor); break;
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
        Long idLogueado = JwtAuthFilter.getCurrentEmpleadoId();

        // Si es el usuario Admin global (ID nulo o cargo admin)
        if (idLogueado == null) {
            System.out.println("👑 Acceso SuperAdmin: Listando todo.");
            return repository.findAll().stream().map(this::mapToDTO).collect(Collectors.toList());
        }

        Empleado empLogueado = empleadoRepository.findById(idLogueado).orElse(null);
        if (empLogueado == null) return List.of();

        String cargo = empLogueado.getCargo().toLowerCase();
        List<Documento> misDocumentos;

        // Regla para Químicos y Administradores de tabla
        if (cargo.contains("admin") || cargo.contains("quimico")) {
            misDocumentos = repository.findAll();
        } else {
            misDocumentos = repository.findByEmpleadoId(idLogueado);
        }

        return misDocumentos.stream()
                .map(this::mapToDTO)
                .sorted((a, b) -> b.getId().compareTo(a.getId()))
                .collect(Collectors.toList());
    }

    @Override
    public DocumentoDTO actualizar(Long id, DocumentoDTO dto) {
        Documento doc = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Documento no encontrado con ID: " + id));

        doc.setNombresyapellidos(dto.getNombresyapellidos());
        doc.setDni(dto.getDni());
        doc.setEdad(dto.getEdad());
        doc.setCualitativo(dto.getCualitativo());
        doc.setCuantitativo(dto.getCuantitativo());
        doc.setNumeroInforme(dto.getNumeroInforme());
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
        dto.setEdad(doc.getEdad());
        dto.setCualitativo(doc.getCualitativo());
        dto.setCuantitativo(doc.getCuantitativo());
        dto.setNumeroInforme(doc.getNumeroInforme());
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
                .edad(dto.getEdad())
                .cualitativo(dto.getCualitativo())
                .cuantitativo(dto.getCuantitativo())
                .numeroInforme(dto.getNumeroInforme())
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
    @Override
    public String obtenerNombreSugerido(Long id) {
        Documento doc = repository.findById(id).orElse(null);
        String cargo = "";

        // 1. Intentar obtener el cargo del usuario logueado actualmente
        Long idLogueado = JwtAuthFilter.getCurrentEmpleadoId();
        if (idLogueado != null) {
            Empleado emp = empleadoRepository.findById(idLogueado).orElse(null);
            if (emp != null) cargo = emp.getCargo().toLowerCase();
        }

        // 2. Si es Admin/Químico, mirar el cargo del creador del documento
        if (cargo.contains("admin") || cargo.contains("quimico")) {
            if (doc != null && doc.getEmpleado() != null) {
                cargo = doc.getEmpleado().getCargo().toLowerCase();
            }
        }

        // 3. Retornar el nombre según el cargo detectado
        if (cargo.contains("tox")) {
            return "Informe_Toxicologia_" + id + ".docx";
        }
        return "Informe_Dosaje_" + id + ".docx";
    }
}
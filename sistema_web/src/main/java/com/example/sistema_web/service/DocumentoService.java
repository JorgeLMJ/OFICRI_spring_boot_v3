package com.example.sistema_web.service;
import com.example.sistema_web.dto.DocumentoDTO;
import java.util.List;

public interface DocumentoService {
    DocumentoDTO crear(DocumentoDTO dto);
    DocumentoDTO obtenerPorId(Long id);
    List<DocumentoDTO> listar();
    DocumentoDTO actualizar(Long id, DocumentoDTO dto);
    void eliminar(Long id);
    byte[] obtenerContenidoArchivo(Long id);
    void actualizarDesdeUrlOnlyOffice(Long id, String urlDescarga, Long empleadoId);
    boolean existeDocumento(Long id);
    void uploadDocumento(Long id, byte[] archivoBytes);
    Long crearDocumentoVacio(Long empleadoId);
    void actualizarCampoEnWord(Long id, String tag, String valor);
    void actualizarTagEnWord(Long id, String tagBuscado, String nuevoValor);
    String obtenerNombreSugerido(Long id);
}

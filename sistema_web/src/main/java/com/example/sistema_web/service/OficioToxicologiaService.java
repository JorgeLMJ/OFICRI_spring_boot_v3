package com.example.sistema_web.service;
import com.example.sistema_web.dto.OficioToxicologiaDTO;
import java.util.List;

public interface OficioToxicologiaService {
    OficioToxicologiaDTO crear(OficioToxicologiaDTO dto);
    OficioToxicologiaDTO obtenerPorId(Long id);
    List<OficioToxicologiaDTO> listar();
    OficioToxicologiaDTO actualizar(Long id, OficioToxicologiaDTO dto);
    void eliminar(Long id);
    byte[] obtenerContenidoArchivo(Long id);
    void actualizarDesdeUrlOnlyOffice(Long id, String urlDescarga, Long documentoId);
    boolean existeOficioToxicologia(Long id);
    void uploadOficioToxicologia(Long id, byte[] archivoBytes);
    Long crearOficioToxicologiaVacio();
    void sincronizarDatosAlWord(Long id);
}

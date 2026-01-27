package com.example.sistema_web.service;
import com.example.sistema_web.dto.OficioDosajeDTO;
import java.util.List;

public interface OficioDosajeService {
    OficioDosajeDTO crear(OficioDosajeDTO dto);
    OficioDosajeDTO obtenerPorId(Long id);
    List<OficioDosajeDTO> listar();
    OficioDosajeDTO actualizar(Long id, OficioDosajeDTO dto);
    void eliminar(Long id);
    byte[] obtenerContenidoArchivo(Long id);
    void actualizarDesdeUrlOnlyOffice(Long id, String urlDescarga, Long documentoId);
    boolean existeOficioDosaje(Long id);
    void uploadOficioDosaje(Long id, byte[] archivoBytes);
    Long crearOficioDosajeVacio();
    void sincronizarDatosAlWord(Long id);
}
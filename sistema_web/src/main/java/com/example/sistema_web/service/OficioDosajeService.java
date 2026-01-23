// src/main/java/com/example/sistema_web/service/OficioDosajeService.java
package com.example.sistema_web.service;
import com.example.sistema_web.dto.OficioDosajeDTO;
import java.util.List;
import java.util.Map;

public interface OficioDosajeService {
    OficioDosajeDTO crear(OficioDosajeDTO dto);
    OficioDosajeDTO obtenerPorId(Long id);
    List<OficioDosajeDTO> listar();
    OficioDosajeDTO actualizar(Long id, OficioDosajeDTO dto);
    void eliminar(Long id);

    // Métodos para OnlyOffice
    byte[] obtenerContenidoArchivo(Long id);
    Map<String, Object> getEditorConfig(Long id, String mode);
    void actualizarDesdeOnlyOffice(Long id, String urlDescarga);
}
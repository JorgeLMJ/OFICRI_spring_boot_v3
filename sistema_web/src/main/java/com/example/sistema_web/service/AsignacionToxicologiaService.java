// src/main/java/com/example/sistema_web/service/AsignacionToxicologiaService.java
package com.example.sistema_web.service;

import com.example.sistema_web.dto.AsignacionToxicologiaDTO;
import java.util.List;

public interface AsignacionToxicologiaService {
    AsignacionToxicologiaDTO crear(AsignacionToxicologiaDTO dto);
    AsignacionToxicologiaDTO obtenerPorId(Long id);
    List<AsignacionToxicologiaDTO> listar();
    AsignacionToxicologiaDTO actualizar(Long id, AsignacionToxicologiaDTO dto);
    void eliminar(Long id);
    void sincronizarDatosAlWord(Long id);
}
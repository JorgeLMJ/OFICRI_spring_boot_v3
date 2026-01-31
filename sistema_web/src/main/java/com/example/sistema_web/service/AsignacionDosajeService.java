package com.example.sistema_web.service;

import com.example.sistema_web.dto.AsignacionDosajeDTO;

import java.util.List;

public interface AsignacionDosajeService {
    AsignacionDosajeDTO crear(AsignacionDosajeDTO dto);
    AsignacionDosajeDTO obtenerPorId(Long id);
    List<AsignacionDosajeDTO> listar();
    AsignacionDosajeDTO actualizar(Long id, AsignacionDosajeDTO dto);
    void eliminar(Long id);
    void sincronizarDatosAlWord(Long id);
}

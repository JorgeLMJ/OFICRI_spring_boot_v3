// src/main/java/com/example/sistema_web/service/EmpleadoService.java
package com.example.sistema_web.service;

import com.example.sistema_web.dto.EmpleadoDTO;
import com.example.sistema_web.model.Empleado;

import java.util.List;

public interface EmpleadoService {
    List<EmpleadoDTO> getAllEmpleados();
    EmpleadoDTO getEmpleadoById(Long id);           // ✅ Devuelve DTO
    EmpleadoDTO createEmpleado(EmpleadoDTO empleadoDTO);
    EmpleadoDTO updateEmpleado(Long id, EmpleadoDTO empleadoDTO);
    void deleteEmpleado(Long id);
    Empleado findByCargo(String cargo);
    void updateEstado(Long id, String estado);
    Empleado getEmpleadoEntityById(Long id);       // ✅ Para uso interno (entidad)
}
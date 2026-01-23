// src/main/java/com/example/sistema_web/repository/AsignacionToxicologiaRepository.java
package com.example.sistema_web.repository;

import com.example.sistema_web.model.AsignacionToxicologia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AsignacionToxicologiaRepository extends JpaRepository<AsignacionToxicologia, Long> {
    long countByEstado(String estado);
    long countByEmpleadoId(Long empleadoId);

    // ✅ NUEVO: Contar por resultado toxicológico
    long countByResultadoToxicologicoContaining(String sustancia);


}
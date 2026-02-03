// src/main/java/com/example/sistema_web/repository/AsignacionToxicologiaRepository.java
package com.example.sistema_web.repository;

import com.example.sistema_web.model.AsignacionToxicologia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AsignacionToxicologiaRepository extends JpaRepository<AsignacionToxicologia, Long> {
    List<AsignacionToxicologia> findByEmisorId(Long emisorId);
    long countByResultadoToxicologicoContaining(String sustancia);
    List<AsignacionToxicologia> findByEmpleadoId(Long idLogueado);
}
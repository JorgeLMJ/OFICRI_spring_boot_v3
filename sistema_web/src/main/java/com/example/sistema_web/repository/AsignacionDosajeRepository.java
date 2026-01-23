// src/main/java/com/example/sistema_web/repository/AsignacionDosajeRepository.java
package com.example.sistema_web.repository;

import com.example.sistema_web.model.AsignacionDosaje;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface AsignacionDosajeRepository extends JpaRepository<AsignacionDosaje, Long> {

    long countByCualitativoContaining(String valor);
}
package com.example.sistema_web.repository;

import com.example.sistema_web.model.Documento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentoRepository extends JpaRepository<Documento, Long> {
    List<Documento> findByEmpleadoId(Long empleadoId);
}
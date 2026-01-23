package com.example.sistema_web.repository;

import com.example.sistema_web.model.Auditoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditoriaRepository extends JpaRepository<Auditoria, Long> {

    // Orden descendente por defecto
    @Query("SELECT a FROM Auditoria a ORDER BY a.fecha DESC")
    List<Auditoria> findAllByOrderByFechaDesc();

    @Query("SELECT a FROM Auditoria a WHERE a.tipoAccion = :tipo ORDER BY a.fecha DESC")
    List<Auditoria> findByTipoAccionOrderByFechaDesc(@Param("tipo") String tipoAccion);

    @Query("SELECT a FROM Auditoria a WHERE a.fecha BETWEEN :inicio AND :fin ORDER BY a.fecha DESC")
    List<Auditoria> findByFechaBetweenOrderByFechaDesc(@Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);
}
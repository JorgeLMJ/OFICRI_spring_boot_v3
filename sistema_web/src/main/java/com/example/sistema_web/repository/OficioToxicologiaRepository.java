package com.example.sistema_web.repository;
import com.example.sistema_web.model.OficioToxicologia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository

public interface OficioToxicologiaRepository extends JpaRepository<OficioToxicologia, Long>  {
    List<OficioToxicologia> findByEmisorId(Long emisorId);
}

package com.example.sistema_web.repository;

import com.example.sistema_web.model.OficioDosaje;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OficioDosajeRepository extends JpaRepository<OficioDosaje, Long> {
    List<OficioDosaje> findByEmisorId(Long emisorId);
}